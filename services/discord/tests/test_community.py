import json, tempfile, unittest
from pathlib import Path
from unittest.mock import patch
from discord_bot.community import Ledger, Community, character_name, profile_alias
from types import SimpleNamespace
from discord_bot import releases

class DeliveryTests(unittest.TestCase):
    def test_runtime_commands_construct_with_installed_discord_api(self):
        with tempfile.TemporaryDirectory() as folder:
            client=SimpleNamespace(config=SimpleNamespace(data_dir=Path(folder)))
            community=Community(client)
            self.assertEqual([c.name for c in community.commands],['empezar','avisos','estado-comunidad','publicar-aviso','perfil'])
            self.assertTrue(community.commands[-2].default_permissions.manage_guild)
            community.ledger.db.close()

    def test_profile_name_cannot_inject_mentions_or_role_prefix(self):
        for name in ['@everyone','[Admin] Yo','', 'a'*19,'nombre\ntexto']:
            with self.assertRaises(ValueError):character_name(name)
        self.assertEqual(character_name(' Juan '),'Juan')

    def test_profile_prefix_uses_actual_roles_not_user_claim(self):
        config=SimpleNamespace(roles={'admin':'7','founder':'8'})
        player=SimpleNamespace(roles=[SimpleNamespace(id=7)])
        self.assertEqual(profile_alias(player,config,'Ash'),'[Admin] Ash')
        player.roles=[]
        self.assertEqual(profile_alias(player,config,'Ash'),'[Entrenador] Ash')

    def test_notice_budget_survives_restart_and_limits_exactly_24_hours(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'community.sqlite';a=Ledger(path)
            self.assertTrue(a.notice('release-a',100000));a.db.close()
            b=Ledger(path)
            self.assertTrue(b.notice('release-a',100001))
            self.assertFalse(b.notice('release-b',186399))
            self.assertTrue(b.notice('release-b',186400));b.db.close()

    def test_process_restart_keeps_sent_version(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'community.sqlite'
            a=Ledger(path);a.reserve('cliente:0.1.3','signed-hash');a.sent('cliente:0.1.3',123);a.db.close()
            b=Ledger(path);row=b.reserve('cliente:0.1.3','signed-hash')
            self.assertEqual(row['status'],'SENT');self.assertEqual(row['message_id'],123);b.db.close()

    def test_same_version_changed_manifest_does_not_become_second_announcement(self):
        with tempfile.TemporaryDirectory() as folder:
            a=Ledger(Path(folder)/'community.sqlite');a.reserve('cliente:0.1.3','first')
            with self.assertRaises(ValueError):a.reserve('cliente:0.1.3','second')
            self.assertEqual(a.reserve('cliente:0.1.3','first')['status'],'PENDING');a.db.close()

    def test_crash_before_send_leaves_pending_not_false_success(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'community.sqlite';a=Ledger(path);a.reserve('cliente:0.1.3','hash');a.db.close()
            b=Ledger(path);self.assertEqual(b.reserve('cliente:0.1.3','hash')['status'],'PENDING');b.db.close()

class PublicationTests(unittest.TestCase):
    def setUp(self):
        self.manifest={'clientBundleVersion':'0.1.3','files':[{'id':'lunaeternal','size':123456,
            'url':'https://pokereport.online/client/release/luna.jar'}]}
        self.latest={'clientBundleVersion':'0.1.3','basePackVersion':'0.2.0',
             'mrpackUrl':'https://pokereport.online/client/release/pack.mrpack'}

    def request(self,url,head=False):
        if head:return 123456
        if url.endswith('.sig'):return b'x'*64
        return json.dumps(self.latest if url.endswith('latest.json') and not url.endswith('client-latest.json') else self.manifest).encode()

    def test_half_published_release_is_not_announced(self):
        self.latest['clientBundleVersion']='0.1.2'
        with patch.object(releases,'request',self.request),patch.object(releases,'verify_signature'):
            with self.assertRaisesRegex(ValueError,'progress'):releases.discover()

    def test_rejected_signature_stops_before_artifact_requests(self):
        seen=[]
        def request(url,head=False):seen.append(head);return self.request(url,head)
        with patch.object(releases,'request',request),patch.object(releases,'verify_signature',side_effect=ValueError('bad signature')):
            with self.assertRaises(ValueError):releases.discover()
        self.assertNotIn(True,seen)

    def test_bad_primary_jar_size_stops_announcement(self):
        def request(url,head=False):return 42 if head else self.request(url,head)
        with patch.object(releases,'request',request),patch.object(releases,'verify_signature'):
            with self.assertRaisesRegex(ValueError,'JAR'):releases.discover()

    def test_valid_release_has_explicit_install_paths(self):
        with patch.object(releases,'request',self.request),patch.object(releases,'verify_signature'):
            release=releases.discover()
        self.assertEqual(release.version,'0.1.3');self.assertIn('0.1.3.zip',release.manual)

    def test_base_update_manual_is_not_derived_from_unchanged_luna_directory(self):
        self.latest['manualUrl']='https://pokereport.online/client/base-030/update-014.zip'
        with patch.object(releases,'request',self.request),patch.object(releases,'verify_signature'):
            release=releases.discover()
        self.assertEqual(release.manual,self.latest['manualUrl'])
        self.assertEqual(release.jar,self.manifest['files'][0]['url'])

    def test_manual_location_must_stay_official(self):
        self.latest['manualUrl']='https://evil.example/update.zip'
        with patch.object(releases,'request',self.request),patch.object(releases,'verify_signature'):
            with self.assertRaisesRegex(ValueError,'Unofficial'):releases.discover()

    def test_reject_external_or_plaintext_downloads(self):
        for url in ['http://pokereport.online/client/a.jar','https://example.com/client/a.jar',
                    'https://pokereport.online.evil.com/client/a.jar','https://pokereport.online@evil.com/client/a.jar']:
            with self.assertRaises(ValueError):releases.official(url)

if __name__=='__main__':unittest.main()
