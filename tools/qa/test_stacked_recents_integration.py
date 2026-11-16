"""Static guards for the opt-in stacked Quickstep prototype.

These checks are not an Android build or a substitute for gesture, dismissal,
rotation, split-screen, and touch hit-test verification on a real device.
"""
from pathlib import Path
import unittest
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[2]
PREFS = ROOT / "src/com/android/launcher3/LauncherPrefs.kt"
RECENTS = ROOT / "quickstep/src/com/android/quickstep/views/RecentsView.java"
TASK = ROOT / "quickstep/src/com/android/quickstep/views/TaskView.kt"
SETTINGS = ROOT / "src/com/android/launcher3/settings/LauncherSettingsScreen.kt"


class StackedRecentsSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.prefs = PREFS.read_text(encoding="utf-8")
        cls.recents = RECENTS.read_text(encoding="utf-8")
        cls.task = TASK.read_text(encoding="utf-8")
        cls.settings = SETTINGS.read_text(encoding="utf-8")

    def test_disabled_by_default(self):
        self.assertIn(
            'val STACKED_RECENTS_ENABLED = backedUpItem("pref_stacked_recents_enabled", false)',
            self.prefs,
        )

    def test_runtime_feature_gate_and_classic_fallback(self):
        self.assertIn("updateStackedRecents(scroll);", self.recents)
        method = self.recents.split("private void updateStackedRecents(int scroll)", 1)[1]
        method = method.split("    @Override", 1)[0]
        for check in (
            "LauncherPrefs.STACKED_RECENTS_ENABLED",
            "mOverviewStateEnabled",
            "!showAsGrid()",
            "!isSplitSelectionActive()",
            "!showAsFullscreen()",
            ".isLargeScreen()",
            "task.setStackedOverviewTransform(0f, 0f, 1f, 0f)",
            "if (!enabled && !mStackedTransformsApplied)",
            "mStackedTransformsApplied = enabled",
        ):
            with self.subTest(check=check):
                self.assertIn(check, method)

    def test_task_transform_is_additive(self):
        for expression in (
            "stackedScale *",
            "animateToIconTranslationX +\n                stackedOffsetX",
            "animateToIconTranslationY +\n                stackedOffsetY",
            "translationZ = stackedDepth",
            "setStackedOverviewTransform(0f, 0f, 1f, 0f)",
        ):
            with self.subTest(expression=expression):
                self.assertIn(expression, self.task)

    def test_experiment_only_in_developer_options(self):
        self.assertIn(
            "if (BuildConfig.IS_STUDIO_BUILD || developerOptionsEnabled) {\n"
            "                    StackedRecentsPreference(context)",
            self.settings,
        )
        self.assertIn("LauncherPrefs.STACKED_RECENTS_ENABLED", self.settings)

    def test_strings_parse_for_en_and_zh(self):
        for path in (
            ROOT / "res/values/uwu_strings.xml",
            ROOT / "res/values-zh-rCN/uwu_strings.xml",
        ):
            with self.subTest(path=str(path)):
                tree = ElementTree.parse(path)
                texts = {e.get("name") for e in tree.getroot().findall("string")}
                self.assertIn("stacked_recents_experimental_title", texts)
                self.assertIn("stacked_recents_experimental_summary", texts)


if __name__ == "__main__":
    unittest.main()
