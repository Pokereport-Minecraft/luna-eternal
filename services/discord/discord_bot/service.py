"""Ticket lifecycle independent from Gateway callbacks, also used in live QA."""
import asyncio
import hashlib
import json
import os
from contextlib import closing
from pathlib import Path
from .config import TYPES, SENSITIVE
from .security import can_access, staff_allowed
from .store import Rejected


class Tickets:
    def __init__(self, config, store, port):
        self.config, self.store, self.port = config, store, port
        self.locks = {}
        self.create_lock = asyncio.Lock()

    def lock(self, ticket_id):
        return self.locks.setdefault(ticket_id, asyncio.Lock())

    def require(self, ticket, actor, roles, action):
        if ticket['guild_id'] != self.config.guild_id or not can_access(self.config, actor, set(roles), ticket, action):
            self.port.log('authorization_denied', ticket_id=ticket['id'], actor_id=actor, action=action)
            raise Rejected('No estás autorizado para esta operación.')

    async def create(self, actor, kind, form, previous=None):
        if kind not in TYPES:
            raise Rejected('Tipo de solicitud desconocido.')
        async with self.create_lock:
            ticket = self.store.reserve(self.config.guild_id, actor, kind, form,
                                        self.config.cooldown, self.config.max_open, previous)
            # A network timeout might mean Discord created the channel. Keep the
            # CREATING reservation; reconcile by stable topic before retrying.
            await self.complete_create(ticket)
            return self.store.ticket(ticket_id=ticket['id'])

    async def complete_create(self, ticket):
        existing = await self.port.find_channel(ticket['id'])
        channel = existing or await self.port.create_channel(ticket)
        await self.port.permissions(dict(ticket, channel_id=channel), closed=False)
        self.store.created(ticket['id'], channel)
        await self.refresh(self.store.ticket(ticket_id=ticket['id']))

    async def refresh(self, ticket):
        message_id = await self.port.controls(ticket)
        self.store.db.execute('UPDATE tickets SET control_message_id=? WHERE id=?', (message_id, ticket['id']))

    async def claim(self, ticket_id, actor, roles):
        async with self.lock(ticket_id):
            ticket = self.store.ticket(ticket_id=ticket_id)
            self.require(ticket, actor, roles, 'claim')
            self.store.claim(ticket_id, actor)
            await self.refresh(self.store.ticket(ticket_id=ticket_id))

    async def release(self, ticket_id, actor, roles):
        async with self.lock(ticket_id):
            ticket = self.store.ticket(ticket_id=ticket_id)
            self.require(ticket, actor, roles, 'release')
            privileged = bool(set(roles) & {int(self.config.roles['founder']), int(self.config.roles['admin'])})
            self.store.release(ticket_id, actor, privileged)
            await self.refresh(self.store.ticket(ticket_id=ticket_id))

    async def transfer(self, ticket_id, actor, roles, target, target_roles):
        async with self.lock(ticket_id):
            ticket = self.store.ticket(ticket_id=ticket_id)
            self.require(ticket, actor, roles, 'transfer')
            if not staff_allowed(self.config, set(target_roles), ticket['type']):
                raise Rejected('El nuevo responsable no pertenece al staff autorizado para este tipo.')
            if ticket['claimed_by'] not in (None, actor) and not set(roles) & {int(self.config.roles['founder']), int(self.config.roles['admin'])}:
                raise Rejected('Solo el responsable o Administración puede transferir.')
            self.store.transfer(ticket_id, actor, target)
            await self.refresh(self.store.ticket(ticket_id=ticket_id))

    async def participant(self, ticket_id, actor, roles, target, add):
        async with self.lock(ticket_id):
            ticket = self.store.ticket(ticket_id=ticket_id)
            self.require(ticket, actor, roles, 'add' if add else 'remove')
            # Additional people in sensitive tickets require explicit admin.
            if ticket['type'] in SENSITIVE and not set(roles) & {int(self.config.roles['founder']), int(self.config.roles['admin'])}:
                raise Rejected('Solo Administración puede cambiar participantes de casos sensibles.')
            if target == ticket['opener_id']:
                raise Rejected('El creador se conserva como participante del ticket activo.')
            people = set(ticket['participants'])
            if add:
                people.add(target)
            else:
                people.discard(target)
            updated = dict(ticket, participants=list(people))
            await self.port.permissions(updated, closed=False)
            self.store.db.execute('UPDATE tickets SET participants=? WHERE id=?', (json.dumps(list(people)), ticket_id))
            self.store.event(ticket_id, actor, 'USER_ADDED' if add else 'USER_REMOVED', {'target': target})

    async def close(self, ticket_id, actor, roles, reason):
        async with self.lock(ticket_id):
            ticket = self.store.ticket(ticket_id=ticket_id)
            self.require(ticket, actor, roles, 'close')
            self.store.begin_close(ticket_id, actor, reason)
            await self.complete_close(self.store.ticket(ticket_id=ticket_id))

    async def complete_close(self, ticket):
        # Freeze writes first. If export fails, leave CLOSING and retain all
        # content. A restart retries export; nothing gets deleted without it.
        await self.port.permissions(ticket, closed=True)
        rows = await self.port.messages(ticket['channel_id'])
        payload = {'schema': 1, 'ticket_id': ticket['id'], 'guild_id': ticket['guild_id'],
                   'type': ticket['type'], 'opener_id': ticket['opener_id'], 'responsible_id': ticket['claimed_by'],
                   'created_at': ticket['created_at'], 'closed_by': ticket['closed_by'],
                   'closed_at': self.store.now(), 'close_reason': ticket['close_reason'],
                   'form': ticket['form'], 'messages': rows}
        raw = json.dumps(payload, ensure_ascii=False, indent=2).encode('utf-8')
        root = self.config.data_dir / 'transcripts'
        root.mkdir(parents=True, exist_ok=True)
        os.chmod(root, 0o700)
        generations = self.store.db.execute('SELECT COUNT(*) FROM transcripts WHERE ticket_id=?', (ticket['id'],)).fetchone()[0]
        path = root / f"ticket-{ticket['id']:06d}-{generations + 1}.json"
        temp = path.with_suffix('.tmp')
        with open(temp, 'wb') as f:
            os.chmod(temp, 0o600)
            f.write(raw)
            f.flush()
            os.fsync(f.fileno())
        os.replace(temp, path)
        self.store.transcript(ticket['id'], path.name, hashlib.sha256(raw).hexdigest(), self.config.retention[ticket['type']])
        await self.port.move_closed(ticket)
        self.store.closed(ticket['id'], self.config.archive_hours)
        await self.refresh(self.store.ticket(ticket_id=ticket['id']))

    async def reopen(self, ticket_id, actor, roles):
        async with self.lock(ticket_id):
            ticket = self.store.ticket(ticket_id=ticket_id)
            self.require(ticket, actor, roles, 'reopen')
            if ticket['purged_at']:
                raise Rejected('Los datos de este ticket ya expiraron; crea una solicitud nueva.')
            if ticket['status'] == 'ARCHIVED':
                # Stable relation, same constraints as a normal opening.
                return await self.create(ticket['opener_id'], ticket['type'], {'Referencia anterior': f"#{ticket['id']:06d}"}, previous=ticket['id'])
            self.store.reopen(ticket_id, actor, self.config.cooldown, self.config.max_open)
            current = self.store.ticket(ticket_id=ticket_id)
            # Persisted OPEN allows reconcile to finish this after API failure.
            await self.port.permissions(current, closed=False)
            await self.port.move_open(current)
            await self.refresh(current)
            return current

    async def reconcile(self):
        rows = list(self.store.db.execute('SELECT id FROM tickets'))
        for row in rows:
            ticket = self.store.ticket(ticket_id=row[0])
            try:
                async with self.lock(ticket['id']):
                    if ticket['status'] == 'CREATING':
                        await self.complete_create(ticket)
                    elif ticket['status'] in ('OPEN', 'CLAIMED', 'CLOSING', 'CLOSED', 'ARCHIVING'):
                        if not await self.port.exists(ticket['channel_id']):
                            self.store.db.execute("UPDATE tickets SET status='ARCHIVED',channel_id=NULL WHERE id=?", (ticket['id'],))
                            self.store.event(ticket['id'], None, 'ERROR_RECOVERED', {'reason': 'channel_missing'})
                            # Do not silently recreate or erase the form.
                            self.store.db.execute('UPDATE tickets SET expires_at=COALESCE(expires_at,?) WHERE id=?', (self.store.now()+self.config.retention[ticket['type']]*86400,ticket['id']))
                        elif ticket['status'] == 'CLOSING':
                            await self.complete_close(ticket)
                        elif ticket['status'] == 'ARCHIVING' or (ticket['status'] == 'CLOSED' and ticket['archive_at'] <= self.store.now()):
                            if not self.store.db.execute('SELECT 1 FROM transcripts WHERE ticket_id=?', (ticket['id'],)).fetchone():
                                raise RuntimeError('Cannot archive without transcript')
                            self.store.db.execute("UPDATE tickets SET status='ARCHIVING' WHERE id=?", (ticket['id'],))
                            await self.port.delete_channel(ticket['channel_id'])
                            self.store.db.execute("UPDATE tickets SET status='ARCHIVED',channel_id=NULL WHERE id=?", (ticket['id'],))
                            self.store.event(ticket['id'], None, 'ARCHIVED')
                        elif ticket['status'] in ('OPEN', 'CLAIMED'):
                            await self.port.permissions(ticket, closed=False)
                            await self.port.move_open(ticket)
                            await self.refresh(ticket)
                        elif ticket['status'] == 'CLOSED':
                            await self.port.permissions(ticket, closed=True)
            except Exception as exc:
                # Only exception class, never request/message bodies.
                self.port.log('reconciliation_error', ticket_id=ticket['id'], error=type(exc).__name__)
                raise

    def expire(self):
        expired = list(self.store.db.execute('SELECT ticket_id,generation,storage_reference FROM transcripts WHERE expires_at<=?', (self.store.now(),)))
        for row in expired:
            path = self.config.data_dir / 'transcripts' / Path(row['storage_reference']).name
            if path.exists():
                path.unlink()
            mirror = self.config.data_dir / 'backups' / 'transcripts' / path.name
            mirror.unlink(missing_ok=True)
            self.store.db.execute('DELETE FROM transcripts WHERE ticket_id=? AND generation=?', (row['ticket_id'], row['generation']))
            self.store.event(row['ticket_id'], None, 'TRANSCRIPT_DELETED', {'generation': row['generation']})
        # Remove expired content from all private backups at the same time.
        for p in (self.config.data_dir / 'backups').glob('*.sqlite'):
            import sqlite3
            with closing(sqlite3.connect(p)) as db:
                db.execute('PRAGMA secure_delete=ON')
                db.execute('DELETE FROM transcripts WHERE expires_at<=?', (self.store.now(),))
                db.execute("UPDATE tickets SET form='{}',close_reason=NULL,opener_id=0,claimed_by=NULL,closed_by=NULL,participants='[]' WHERE expires_at<=?", (self.store.now(),))
                db.execute('DELETE FROM ticket_events WHERE expires_at<=?', (self.store.now(),))
                db.commit()
                db.execute('VACUUM')
        self.store.db.execute("UPDATE tickets SET form='{}',close_reason=NULL,opener_id=0,claimed_by=NULL,closed_by=NULL,participants='[]',purged_at=? WHERE expires_at<=? AND status='ARCHIVED' AND purged_at IS NULL", (self.store.now(), self.store.now()))
        self.store.db.execute('DELETE FROM ticket_events WHERE expires_at<=?', (self.store.now(),))
        self.store.db.execute('DELETE FROM request_limits WHERE last_attempt<?', (self.store.now()-86400,))
        if expired:
            self.store.db.execute('PRAGMA wal_checkpoint(TRUNCATE)')

    def backup(self, destination):
        """Copy verified transcripts before the SQLite snapshot that references them."""
        import hashlib
        import os
        import shutil
        root = Path(destination).parent / 'transcripts'
        root.mkdir(parents=True, exist_ok=True, mode=0o700)
        for row in self.store.db.execute('SELECT storage_reference,hash FROM transcripts WHERE expires_at>?', (self.store.now(),)):
            source = self.config.data_dir / 'transcripts' / Path(row['storage_reference']).name
            if hashlib.sha256(source.read_bytes()).hexdigest() != row['hash']:
                raise RuntimeError('Transcript backup integrity failed')
            target = root / source.name
            temporary = target.with_suffix('.tmp')
            shutil.copyfile(source, temporary)
            os.chmod(temporary, 0o600)
            os.replace(temporary, target)
        self.store.backup(destination)
