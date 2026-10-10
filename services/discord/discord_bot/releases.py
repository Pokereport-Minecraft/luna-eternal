"""Verify the same signed client manifest used by the updater before announcing."""
import base64, hashlib, json, re, shutil, subprocess, tempfile, urllib.request
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse

PUBLIC_KEY='MCowBQYDK2VwAyEAKI58PNfegf7pWop3OTa6ttewQ1CrkVQMU1dgAXLzP/s='
ROOT='https://pokereport.online/client/'

def official(url):
    parsed=urlparse(url)
    if parsed.scheme!='https' or parsed.netloc!='pokereport.online' or not parsed.path.startswith('/client/'):
        raise ValueError('Unofficial download location')
    return url

def request(url, head=False):
    official(url)
    req=urllib.request.Request(url,method='HEAD' if head else 'GET',
                              headers={'User-Agent':'PokeReportCommunity/1.0','Cache-Control':'no-cache'})
    with urllib.request.urlopen(req,timeout=20) as result:
        official(result.url)
        if head:return int(result.headers.get('Content-Length',0))
        data=result.read(2_000_001)
        if len(data)>2_000_000:raise ValueError('Manifest too large')
        return data

def verify_signature(data, signature):
    if len(signature)!=64:raise ValueError('Invalid signature size')
    executable=shutil.which('openssl')
    if not executable:raise RuntimeError('OpenSSL required for signature verification')
    with tempfile.TemporaryDirectory() as directory:
        root=Path(directory)
        (root/'key.der').write_bytes(base64.b64decode(PUBLIC_KEY))
        (root/'manifest').write_bytes(data);(root/'signature').write_bytes(signature)
        result=subprocess.run([executable,'pkeyutl','-verify','-rawin','-pubin','-keyform','DER',
                               '-inkey',str(root/'key.der'),'-in',str(root/'manifest'),
                               '-sigfile',str(root/'signature')],capture_output=True,timeout=15)
        if result.returncode:raise ValueError('Manifest signature rejected')

@dataclass(frozen=True)
class Release:
    version:str
    digest:str
    title:str
    summary:str
    mrpack:str
    manual:str | None
    base:str
    jar:str | None = None

def discover():
    raw=request(ROOT+'client-latest.json')
    verify_signature(raw,request(ROOT+'client-latest.json.sig'))
    manifest=json.loads(raw);latest=json.loads(request(ROOT+'latest.json'))
    version=manifest['clientBundleVersion']
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?',version):raise ValueError('Invalid version')
    if latest['clientBundleVersion']!=version:raise ValueError('Publication still in progress')
    own=[entry for entry in manifest['files'] if entry['id']=='lunaeternal']
    if len(own)!=1:raise ValueError('Ambiguous primary artifact')
    entry=own[0]
    if request(entry['url'],head=True)!=entry['size']:raise ValueError('Primary JAR unavailable')
    mrpack=official(latest['mrpackUrl'])
    if request(mrpack,head=True)<1000:raise ValueError('MRPACK unavailable')
    directory=entry['url'].rsplit('/',1)[0]+'/'
    manual=official(latest.get('manualUrl') or directory+'PokeReport-Actualizacion-Cliente-'+version+'.zip')
    try:
        if request(manual,head=True)<1000:manual=None
    except urllib.error.HTTPError as exc:
        if exc.code!=404:raise
        manual=None
    notes=manifest.get('releaseNotes') or {}
    return Release(version,hashlib.sha256(raw).hexdigest(),str(notes.get('title') or 'Actualización de cliente')[:180],
                   str(notes.get('summary') or 'Actualización verificada del cliente disponible.')[:1200],
                   mrpack,manual,str(latest['basePackVersion']),official(entry['url']))
