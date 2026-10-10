"""All operations check backend identity, not just command visibility."""
from .config import TYPES, SENSITIVE


def staff_allowed(config, role_ids, kind, historical=False):
    names = ('founder', 'admin') if historical and kind in SENSITIVE else TYPES[kind]['roles']
    return any(int(config.roles[name]) in role_ids for name in names)


def can_access(config, user_id, role_ids, ticket, action):
    historical = ticket['status'] in ('CLOSING', 'CLOSED', 'ARCHIVED', 'ARCHIVING')
    staff = staff_allowed(config, role_ids, ticket['type'], historical=historical)
    if action in ('claim', 'release', 'transfer', 'add', 'remove'):
        return ticket['status'] in ('OPEN', 'CLAIMED') and staff
    if action == 'transcript':
        return staff_allowed(config, role_ids, ticket['type'], historical=True)
    if action in ('info', 'reopen'):
        return staff or (user_id == ticket['opener_id'] and action == 'info' and not (historical and ticket['type'] in SENSITIVE))
    if action == 'close':
        return ticket['status'] in ('OPEN', 'CLAIMED') and (staff or user_id == ticket['opener_id'])
    return False


def overwrite_spec(config, bot_id, ticket, closed=False):
    # (id, type, allow, deny). No inherited staff category grants.
    view, history, send = 1 << 10, 1 << 16, 1 << 11
    attachment, embed = 1 << 15, 1 << 14
    forbidden = (1 << 35) | (1 << 36) | (1 << 38) | (1 << 17) | (1 << 50)
    read = view | history
    write = read | send | attachment | embed
    spec = [(config.guild_id, 0, 0, view | forbidden)]
    if config.roles.get('carl'):
        spec.append((int(config.roles['carl']), 0, 0, view))
    bot_allow = write | (1 << 4) | (1 << 13) | (1 << 28)
    spec.append((bot_id, 1, bot_allow, 0))
    sensitive_closed = closed and ticket['type'] in SENSITIVE
    allowed_roles = ('founder', 'admin') if sensitive_closed else TYPES[ticket['type']]['roles']
    for name in allowed_roles:
        spec.append((int(config.roles[name]), 0, read if closed else write, (send | forbidden) if closed else forbidden))
    if not sensitive_closed:
        spec.append((ticket['opener_id'], 1, read if closed else write, (send | forbidden) if closed else forbidden))
    if not closed:
        for user_id in ticket.get('participants', []):
            spec.append((int(user_id), 1, write, forbidden))
    return spec
