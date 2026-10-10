"""SQLite WAL, durable sagas and transactional limits. Never logs content."""
import json
import sqlite3
import time
from contextlib import contextmanager, closing
from pathlib import Path


class Rejected(Exception):
    pass


SCHEMA = '''
CREATE TABLE IF NOT EXISTS migrations(version INTEGER PRIMARY KEY, applied_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS tickets(
 id INTEGER PRIMARY KEY AUTOINCREMENT, guild_id INTEGER NOT NULL,
 channel_id INTEGER UNIQUE, opener_id INTEGER NOT NULL, type TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('CREATING','OPEN','CLAIMED','CLOSING','CLOSED','ARCHIVING','ARCHIVED')),
 claimed_by INTEGER, created_at INTEGER NOT NULL, closed_at INTEGER,
 closed_by INTEGER, close_reason TEXT, form TEXT NOT NULL DEFAULT '{}',
 previous_ticket_id INTEGER REFERENCES tickets(id), control_message_id INTEGER,
 archive_at INTEGER, expires_at INTEGER, purged_at INTEGER, participants TEXT NOT NULL DEFAULT '[]');
CREATE UNIQUE INDEX IF NOT EXISTS active_user_type ON tickets(guild_id,opener_id,type)
 WHERE status IN ('CREATING','OPEN','CLAIMED','CLOSING');
CREATE TABLE IF NOT EXISTS ticket_events(id INTEGER PRIMARY KEY AUTOINCREMENT,
 ticket_id INTEGER REFERENCES tickets(id), actor_id INTEGER, event_type TEXT NOT NULL,
 payload TEXT NOT NULL DEFAULT '{}', created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS transcripts(ticket_id INTEGER NOT NULL REFERENCES tickets(id),
 generation INTEGER NOT NULL, storage_reference TEXT NOT NULL, hash TEXT NOT NULL,
 created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL,
 PRIMARY KEY(ticket_id,generation));
CREATE TABLE IF NOT EXISTS managed_panels(panel_key TEXT PRIMARY KEY, channel_id INTEGER NOT NULL,
 message_id INTEGER NOT NULL, version INTEGER NOT NULL, updated_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS request_limits(user_id INTEGER PRIMARY KEY,last_attempt INTEGER NOT NULL);
'''


class Store:
    def __init__(self, path, clock=time.time, audit_days=365):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.clock = clock
        self.audit_days = audit_days
        self.db = sqlite3.connect(self.path, isolation_level=None, timeout=10)
        self.db.row_factory = sqlite3.Row
        self.db.execute('PRAGMA journal_mode=WAL')
        self.db.execute('PRAGMA synchronous=FULL')
        self.db.execute('PRAGMA foreign_keys=ON')
        self.db.execute('PRAGMA secure_delete=ON')
        self.db.executescript(SCHEMA)
        self.db.execute('INSERT OR IGNORE INTO migrations VALUES(1,?)', (self.now(),))
        if not self.db.execute('SELECT 1 FROM migrations WHERE version=2').fetchone():
            with self.transaction():
                self.db.execute('ALTER TABLE ticket_events ADD COLUMN delivered_message_id INTEGER')
                self.db.execute('INSERT INTO migrations VALUES(2,?)', (self.now(),))
        if not self.db.execute('SELECT 1 FROM migrations WHERE version=3').fetchone():
            self.db.executescript(f'''
            BEGIN IMMEDIATE;
            CREATE TABLE welcome_events(
              id INTEGER PRIMARY KEY, guild_id INTEGER NOT NULL, user_id INTEGER NOT NULL,
              joined_at TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'PENDING',
              message_id INTEGER, created_at INTEGER NOT NULL,
              UNIQUE(guild_id,user_id,joined_at));
            CREATE TABLE contributions(
              id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL,
              authorized_by INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
              category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
              state TEXT NOT NULL DEFAULT 'DRAFT', dedupe_key TEXT NOT NULL UNIQUE,
              message_id INTEGER, channel_id INTEGER, revision INTEGER NOT NULL DEFAULT 0,
              pending_data TEXT NOT NULL DEFAULT '{{}}');
            INSERT INTO migrations VALUES(3,{self.now()});
            COMMIT;
            ''')
        if self.db.execute('PRAGMA quick_check').fetchone()[0] != 'ok':
            raise RuntimeError('SQLite integrity failure')

    def now(self):
        return int(self.clock())

    @contextmanager
    def transaction(self):
        self.db.execute('BEGIN IMMEDIATE')
        try:
            yield
        except BaseException:
            self.db.execute('ROLLBACK')
            raise
        else:
            self.db.execute('COMMIT')

    def event(self, ticket_id, actor, event, payload=None):
        self.db.execute('INSERT INTO ticket_events(ticket_id,actor_id,event_type,payload,created_at,expires_at) VALUES(?,?,?,?,?,?)',
                        (ticket_id, actor, event, json.dumps(payload or {}), self.now(), self.now() + self.audit_days * 86400))

    def ticket(self, ticket_id=None, channel_id=None):
        key, value = ('id', ticket_id) if ticket_id is not None else ('channel_id', channel_id)
        row = self.db.execute(f'SELECT * FROM tickets WHERE {key}=?', (value,)).fetchone()
        if not row:
            raise Rejected('Ticket no encontrado.')
        result = dict(row)
        result['participants'] = json.loads(result['participants'])
        result['form'] = json.loads(result['form'])
        return result

    def active(self, user_id=None):
        query = "SELECT id FROM tickets WHERE status IN ('CREATING','OPEN','CLAIMED','CLOSING')"
        params = ()
        if user_id is not None:
            query += ' AND opener_id=?'
            params = (user_id,)
        return [self.ticket(ticket_id=x[0]) for x in self.db.execute(query, params)]

    def reserve(self, guild_id, user_id, kind, form, cooldown=60, max_open=2, previous=None, bypass_cooldown=False):
        with self.transaction():
            existing = self.db.execute("SELECT id FROM tickets WHERE guild_id=? AND opener_id=? AND type=? AND status IN ('CREATING','OPEN','CLAIMED','CLOSING')", (guild_id, user_id, kind)).fetchone()
            if existing:
                raise Rejected(f'Ya tienes un ticket activo de este tipo: #{existing[0]:06d}.')
            if len(self.active(user_id)) >= max_open:
                raise Rejected(f'El máximo es {max_open} tickets activos.')
            last = self.db.execute('SELECT last_attempt FROM request_limits WHERE user_id=?', (user_id,)).fetchone()
            if not bypass_cooldown and last and self.now() - last[0] < cooldown:
                raise Rejected('Espera 60 segundos entre solicitudes.')
            self.db.execute('INSERT INTO request_limits VALUES(?,?) ON CONFLICT(user_id) DO UPDATE SET last_attempt=excluded.last_attempt', (user_id, self.now()))
            cursor = self.db.execute("INSERT INTO tickets(guild_id,opener_id,type,status,created_at,form,previous_ticket_id) VALUES(?,?,?,'CREATING',?,?,?)", (guild_id, user_id, kind, self.now(), json.dumps(form), previous))
            ticket_id = cursor.lastrowid
            self.event(ticket_id, user_id, 'RESERVED')
        return self.ticket(ticket_id=ticket_id)

    def created(self, ticket_id, channel_id):
        with self.transaction():
            self.db.execute("UPDATE tickets SET channel_id=?,status='OPEN' WHERE id=? AND status='CREATING'", (channel_id, ticket_id))
            self.event(ticket_id, None, 'CREATED')

    def claim(self, ticket_id, actor):
        with self.transaction():
            changed = self.db.execute("UPDATE tickets SET claimed_by=?,status='CLAIMED' WHERE id=? AND status='OPEN' AND claimed_by IS NULL", (actor, ticket_id)).rowcount
            if not changed:
                raise Rejected('Este ticket ya fue reclamado o no está abierto.')
            self.event(ticket_id, actor, 'CLAIMED')

    def release(self, ticket_id, actor, privileged=False):
        with self.transaction():
            ticket = self.ticket(ticket_id=ticket_id)
            if ticket['status'] != 'CLAIMED' or (ticket['claimed_by'] != actor and not privileged):
                raise Rejected('Solo el responsable o Administración puede liberar el ticket.')
            self.db.execute("UPDATE tickets SET claimed_by=NULL,status='OPEN' WHERE id=?", (ticket_id,))
            self.event(ticket_id, actor, 'RELEASED')

    def transfer(self, ticket_id, actor, target):
        with self.transaction():
            ticket = self.ticket(ticket_id=ticket_id)
            if ticket['status'] not in ('OPEN', 'CLAIMED'):
                raise Rejected('El ticket no está abierto.')
            self.db.execute("UPDATE tickets SET claimed_by=?,status='CLAIMED' WHERE id=?", (target, ticket_id))
            self.event(ticket_id, actor, 'TRANSFERRED', {'target': target})

    def begin_close(self, ticket_id, actor, reason):
        if not reason or not reason.strip():
            raise Rejected('El motivo del cierre es obligatorio.')
        with self.transaction():
            changed = self.db.execute("UPDATE tickets SET status='CLOSING',closed_by=?,close_reason=? WHERE id=? AND status IN ('OPEN','CLAIMED')", (actor, reason.strip()[:1000], ticket_id)).rowcount
            if not changed:
                raise Rejected('El ticket ya está cerrándose o cerrado.')
            self.event(ticket_id, actor, 'CLOSE_REQUESTED')

    def closed(self, ticket_id, hours):
        with self.transaction():
            self.db.execute("UPDATE tickets SET status='CLOSED',closed_at=?,archive_at=? WHERE id=? AND status='CLOSING'", (self.now(), self.now() + hours * 3600, ticket_id))
            self.event(ticket_id, None, 'CLOSED')

    def transcript(self, ticket_id, path, digest, days):
        with self.transaction():
            generation = self.db.execute('SELECT COALESCE(MAX(generation),0)+1 FROM transcripts WHERE ticket_id=?', (ticket_id,)).fetchone()[0]
            expires = self.now() + days * 86400
            self.db.execute('INSERT INTO transcripts VALUES(?,?,?,?,?,?)', (ticket_id, generation, str(path), digest, self.now(), expires))
            self.db.execute('UPDATE tickets SET expires_at=? WHERE id=?', (expires, ticket_id))
            self.event(ticket_id, None, 'TRANSCRIPT_CREATED', {'generation': generation, 'hash': digest})

    def reopen(self, ticket_id, actor, cooldown, max_open):
        with self.transaction():
            old = self.ticket(ticket_id=ticket_id)
            if old['status'] != 'CLOSED':
                raise Rejected('Solo un ticket cerrado con canal puede reabrirse.')
            if len(self.active(old['opener_id'])) >= max_open:
                raise Rejected('El creador ya alcanzó el máximo de tickets activos.')
            duplicate = self.db.execute("SELECT id FROM tickets WHERE opener_id=? AND type=? AND status IN ('CREATING','OPEN','CLAIMED','CLOSING')", (old['opener_id'], old['type'])).fetchone()
            if duplicate:
                raise Rejected('El creador tiene otro ticket activo de este tipo.')
            self.db.execute("UPDATE tickets SET status='OPEN',claimed_by=NULL,archive_at=NULL,closed_at=NULL,closed_by=NULL,close_reason=NULL WHERE id=?", (ticket_id,))
            self.event(ticket_id, actor, 'REOPENED')

    def backup(self, destination):
        # This copy includes private data and inherits retention restrictions.
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with closing(sqlite3.connect(destination)) as target:
            self.db.backup(target)
            if target.execute('PRAGMA integrity_check').fetchone()[0] != 'ok':
                raise RuntimeError('Backup integrity failed')

    def close(self):
        self.db.close()
