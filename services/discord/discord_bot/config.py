import json
import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Config:
    guild_id: int
    application_id: int
    roles: dict
    channels: dict
    data_dir: Path
    cooldown: int = 60
    max_open: int = 2
    archive_hours: int = 72
    retention: dict = None
    policy_approved: bool = False
    publish_panel: bool = False
    message_content: bool = False
    audit_days: int = 365
    welcome_enabled: bool = False
    recognition_enabled: bool = False
    community_enabled: bool = False

    @classmethod
    def load(cls):
        path = Path(os.environ.get('POKEREPORT_CONFIG', '/etc/pokereport-discord/config.json'))
        raw = json.loads(path.read_text(encoding='utf-8'))
        raw['data_dir'] = Path(raw['data_dir'])
        result = cls(**raw)
        if not result.policy_approved:
            raise RuntimeError('Retention/privacy policy must be approved before runtime activation')
        return result


TYPES = {
    'help': {'label': 'Ayuda general', 'emoji': '🧭', 'roles': ('founder', 'admin', 'moderator')},
    'bug': {'label': 'Reportar bug', 'emoji': '🐞', 'roles': ('founder', 'admin', 'developer')},
    'report': {'label': 'Denuncia privada', 'emoji': '🛡️', 'roles': ('founder', 'admin', 'moderator')},
    'admin': {'label': 'Administración', 'emoji': '📩', 'roles': ('founder', 'admin')},
}
SENSITIVE = {'report', 'admin'}
GOLD = 0xFFB000
