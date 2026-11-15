"""Offline source contracts: python3 -m unittest discover -s tests -v."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'src/android/app'
MAIN = APP / 'src/main'
JAVA = MAIN / 'java/com/openminis/app'
ID = 'io.github.stevenking123.auraminis'
A = '{http://schemas.android.com/apk/res/android}'

class ReleaseIdentityContracts(unittest.TestCase):
    def test_release_flags_and_unsigned_identity(self):
        gradle = (APP / 'build.gradle.kts').read_text()
        self.assertIn('namespace = "com.openminis.app"', gradle)
        self.assertIn(f'applicationId = "{ID}"', gradle)
        self.assertIn('versionCode = 44', gradle)
        self.assertIn('versionName = "1.0.0"', gradle)
        release = gradle.split('        release {', 1)[1].split('    flavorDimensions', 1)[0]
        for flag in ['isDebuggable = false', 'isMinifyEnabled = true', 'signingConfig = null',
                     'buildConfigField("boolean", "DEV_TOOLS", "false")']:
            self.assertIn(flag, release)
        self.assertNotIn('signingConfigs.getByName("debug")', release)
        for flag in ['SELF_UPDATE', 'AUTO_UPDATE_CHECK', 'IN_APP_INSTALL']:
            self.assertEqual(['false', 'false'], re.findall(
                r'buildConfigField\("boolean", "' + flag + r'", "(true|false)"\)', gradle))

    def test_manifest_providers_and_webapp_action(self):
        manifest = ET.parse(MAIN / 'AndroidManifest.xml').getroot()
        app = manifest.find('application')
        authorities = {p.get(A + 'authorities') for p in app.findall('provider')}
        self.assertIn('${applicationId}.fileprovider', authorities)
        self.assertIn('${applicationId}.shizuku', authorities)
        actions = {p.get(A + 'name') for p in app.iter('action')}
        self.assertIn('${applicationId}.action.OPEN_WEBAPP', actions)
        self.assertNotIn('com.openminis.app.action.OPEN_WEBAPP', actions)
        self.assertIn('minis', {p.get(A + 'scheme') for p in app.iter('data')})

    def test_static_shortcuts_preserve_class_identity(self):
        intents = ET.parse(MAIN / 'res/xml/shortcuts.xml').getroot().findall('shortcut/intent')
        self.assertEqual(3, len(intents))
        for intent in intents:
            self.assertEqual(ID, intent.get(A + 'targetPackage'))
            self.assertEqual('com.openminis.app.MainActivity', intent.get(A + 'targetClass'))
            self.assertTrue(intent.get(A + 'data').startswith('minis://'))

    def test_runtime_components_split_package_and_class(self):
        icons = (JAVA / 'data/repository/AppIconRepository.kt').read_text()
        self.assertIn('CLASS_NAMESPACE = "com.openminis.app"', icons)
        self.assertIn('ComponentName(ctx.packageName, variant.aliasClass)', icons)
        a11y = (JAVA / 'accessibility/MinisAccessibilityService.kt').read_text()
        self.assertIn('com.openminis.app.BuildConfig.APPLICATION_ID,', a11y)
        self.assertIn('MinisAccessibilityService::class.java.name', a11y)
        self.assertIn('flattenToShortString()', a11y)
        self.assertNotIn('"com.openminis.app/.accessibility', a11y)

    def test_service_and_receiver_actions_are_application_scoped(self):
        paths = ['service/AgentForegroundService.kt', 'service/BackupForegroundService.kt',
                 'scheduled/ScheduledTaskManager.kt', 'memory/AdvancedMemoryIdleScheduler.kt',
                 'webapp/WebAppActivity.kt']
        for path in paths:
            src = (JAVA / path).read_text()
            self.assertRegex(src, r'const val ACTION\w* = com\.openminis\.app\.BuildConfig\.APPLICATION_ID \+')
        self.assertIn('${app.packageName}.USER_ASK_ANSWER',
                      (JAVA / 'notification/UserAskNotifier.kt').read_text())

    def test_backup_paths_stay_package_relative_and_secrets_excluded(self):
        for filename in ['backup_rules.xml', 'data_extraction_rules.xml']:
            tree = ET.parse(MAIN / 'res/xml' / filename).getroot()
            excludes = list(tree.iter('exclude'))
            self.assertTrue(excludes)
            self.assertTrue(all(not e.get('path').startswith('/') for e in excludes))
            self.assertIn('provider_secrets.xml', {e.get('path') for e in excludes})
            self.assertIn('env_var_values.xml', {e.get('path') for e in excludes})
        watermark = (JAVA / 'diagnostics/ResourceWatermark.kt').read_text()
        self.assertIn('BuildConfig.APPLICATION_ID', watermark)
        self.assertNotIn('"/com.openminis.app/"', watermark)

if __name__ == '__main__':
    unittest.main()
