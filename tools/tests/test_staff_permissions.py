import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import uuid

spec = importlib.util.spec_from_file_location('staff_permissions', Path(__file__).parents[1] / 'staff_permissions.py')
staff = importlib.util.module_from_spec(spec)
spec.loader.exec_module(staff)

class PermissionSafetyTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name).resolve()
        self.first, self.other = str(uuid.uuid4()), str(uuid.uuid4())
        self.ops = [{'uuid': self.first, 'name': 'QA_First', 'level': 4, 'bypassesPlayerLimit': False},
                    {'uuid': self.other, 'name': 'QA_Other', 'level': 3, 'bypassesPlayerLimit': False}]
        self.grants = {self.first: (staff.EDIT, staff.MODE, staff.COMMANDS)}
        (self.root / 'ops.json').write_text(json.dumps(self.ops))
        self.file = self.root / 'config/lunaeternal/world-edit.properties'
        self.file.parent.mkdir(parents=True)
        self.file.write_text(f'{self.first}={staff.EDIT},{staff.MODE},{staff.COMMANDS}\n')

    def test_downgrade_removes_previous_edit_and_command_grants(self):
        ops, grants = staff.updated_state(self.ops, self.grants, self.first, 'QA_First', 'moderator')
        self.assertNotIn(self.first, grants)
        self.assertEqual(next(x for x in ops if x['uuid'] == self.first)['level'], 3)
        self.assertIn(self.ops[1], ops)

    def test_developer_does_not_implicitly_get_world_edit(self):
        _, grants = staff.updated_state(self.ops, self.grants, self.first, 'QA_First', 'developer')
        self.assertEqual(grants[self.first], (staff.COMMANDS,))

    def test_revoke_preserves_other_people_and_makes_backup(self):
        ops, grants = staff.updated_state(self.ops, self.grants, self.first)
        backup = staff.save_state(self.root, ops, grants)
        current_ops, current_grants = staff.read_state(self.root)
        self.assertEqual(current_ops, [self.ops[1]])
        self.assertNotIn(self.first, current_grants)
        self.assertEqual(json.loads((backup / 'ops.json').read_text()), self.ops)

    def test_invalid_configuration_is_rejected_without_mutation(self):
        self.file.write_text(f'{self.first}=*\n')
        before = (self.root / 'ops.json').read_bytes()
        with self.assertRaises(ValueError):
            staff.read_state(self.root)
        self.assertEqual((self.root / 'ops.json').read_bytes(), before)

    def test_second_file_failure_restores_both_files(self):
        originals = [(self.root / 'ops.json').read_bytes(), self.file.read_bytes()]
        ops, grants = staff.updated_state(self.ops, self.grants, self.first)
        real_replace = staff.replace_bytes
        count = 0
        def fail_second(path, value):
            nonlocal count
            count += 1
            if count == 2:
                raise OSError('simulated write failure')
            real_replace(path, value)
        with patch.object(staff, 'replace_bytes', side_effect=fail_second):
            with self.assertRaises(OSError):
                staff.save_state(self.root, ops, grants)
        self.assertEqual([(self.root / 'ops.json').read_bytes(), self.file.read_bytes()], originals)

    def test_running_or_unrelated_container_is_rejected(self):
        for running, source in [(True, str(self.root)), (False, str(self.root / 'other'))]:
            info = [{'State': {'Running': running}, 'Mounts': [{'Source': source}]}]
            with patch.object(staff.subprocess, 'check_output', return_value=json.dumps(info)):
                with self.assertRaises(ValueError):
                    staff.verify_stopped_container('qa', self.root)

if __name__ == '__main__':
    unittest.main()
