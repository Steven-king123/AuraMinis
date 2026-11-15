#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/src/android"
./gradlew --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process \
 :app:testGithubReleaseUnitTest \
 --tests 'com.openminis.app.memory.*' \
 --tests 'com.openminis.app.ui.chat.DisplayMath*Test' \
 --tests 'com.openminis.app.ui.chat.ChatBackground*Test' \
 --tests 'com.openminis.app.ui.chat.ChatGlass*Test' \
 --tests 'com.openminis.app.ui.chat.ChatIme*Test' \
 --tests 'com.openminis.app.ui.chat.ChatComposerGlassOwnershipTest' \
 --tests 'com.openminis.app.ui.chat.ChatMessageBackplateStyleTest' \
 --tests 'com.openminis.app.ui.chat.ReadingAnchorTest' \
 --tests 'com.openminis.app.ui.chat.ChatNestedScrollFollowTest' \
 --tests 'com.openminis.app.ui.settings.AdvancedMemory*Test' \
 --tests 'com.openminis.app.ui.settings.SettingsHelpPresentationTest' \
 --tests 'com.openminis.app.ui.settings.AppearanceCopy43Test' \
 --tests 'com.openminis.app.ui.settings.MemoryModelPickerPolicyTest' \
 --tests 'com.openminis.app.ui.settings.Aura*Test' \
 --tests 'com.openminis.app.release.ReleaseIdentityTest' \
 :app:assembleGithubRelease
