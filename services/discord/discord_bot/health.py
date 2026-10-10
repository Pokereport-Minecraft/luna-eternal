import json
import shutil
from pathlib import Path


def metrics(client):
    now=client.store.now()
    root=client.config.data_dir
    usage=shutil.disk_usage(root)
    status_path=root/'backup-status.json'
    backup=json.loads(status_path.read_text()) if status_path.exists() else {}
    created=backup.get('created_at')
    age=now-created if isinstance(created,int) else None
    exists=bool(backup.get('file')) and (root/'backups'/Path(backup['file']).name).is_file()
    return {'latency_ms':round(client.latency*1000,1) if client.latency==client.latency else None,
            'disk_free_bytes':usage.free,'disk_ok':usage.free>=256*1024*1024,
            'backup':{'created_at':created,'age_seconds':age,'valid':bool(backup.get('verified')) and exists,
                      'recent':age is not None and 0<=age<=26*3600},
            'welcome_enabled':client.config.welcome_enabled,
            'welcome_renderer_errors':getattr(getattr(client,'welcome',None),'errors',0),
            'last_retention_at':getattr(client,'last_retention_at',None),
            'last_maintenance_error':getattr(client,'last_maintenance_error',None)}
