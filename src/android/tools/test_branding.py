"""Offline, dependency-free checks for the Android-only AuraMinis branding patch.
Run: python3 tools/test_branding.py
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
ABOUT = ROOT / 'app/src/main/java/com/openminis/app/ui/settings/AboutScreen.kt'


def strings(locale):
    return {item.attrib['name']: ''.join(item.itertext())
            for item in ET.parse(RES / locale / 'strings.xml').getroot()
            if item.tag == 'string'}


class AuraMinisBrandTests(unittest.TestCase):
    def test_app_name_is_identical_in_every_locale(self):
        locales = sorted(RES.glob('values*/strings.xml'))
        self.assertGreater(len(locales), 20)
        for resource in locales:
            with self.subTest(locale=resource.parent.name):
                self.assertEqual('AuraMinis', strings(resource.parent.name)['app_name'])

    def test_primary_english_and_chinese_brand_copy(self):
        for locale in ('values', 'values-zh'):
            copy = strings(locale)
            for key in ('settings_about_minis', 'about_minis_tagline',
                        'onboarding_welcome_title', 'sessionlist_welcome_title'):
                with self.subTest(locale=locale, key=key):
                    self.assertIn('AuraMinis', copy[key])
            self.assertIn('OpenMinis', copy['about_fork_notice'])
            self.assertIn('unofficial' if locale == 'values' else '非官方', copy['about_fork_notice'])
            self.assertIn('copyright' if locale == 'values' else '版权', copy['about_upstream_attribution'])

    def test_about_keeps_upstream_and_separates_fork(self):
        source = ABOUT.read_text()
        self.assertIn('https://github.com/Steven-king123/AuraMinis', source)
        self.assertIn('https://github.com/OpenMinis/OpenMinis', source)
        for key in ('about_fork_notice', 'about_upstream_attribution',
                    'about_fork_repository', 'about_upstream_repository'):
            self.assertIn('R.string.' + key, source)
            self.assertIn(key, strings('values'))
            self.assertIn(key, strings('values-zh'))
        self.assertIn('CheckUpdateSection()', source)
        self.assertIn('getApplicationIcon(context.packageName)', source)

    def test_existing_tool_and_assistant_copy_remains_intact(self):
        for locale in ('values', 'values-zh'):
            copy = strings(locale)
            self.assertIn('minis-model-use', copy['agent_loop_section_footer'])
            self.assertIn('android-shizuku-cli', copy['shizuku_status_footer'])
            self.assertEqual('Minis Skills', copy['skills_browser_title'])
            self.assertIn('Minis', copy['user_ask_left_to_minis'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
