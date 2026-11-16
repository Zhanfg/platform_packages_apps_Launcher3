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
UTILS = ROOT / "quickstep/src/com/android/quickstep/views/RecentsViewUtils.kt"
SETTINGS = ROOT / "src/com/android/launcher3/settings/LauncherSettingsScreen.kt"


class StackedRecentsSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.prefs = PREFS.read_text(encoding="utf-8")
        cls.recents = RECENTS.read_text(encoding="utf-8")
        cls.task = TASK.read_text(encoding="utf-8")
        cls.utils = UTILS.read_text(encoding="utf-8")
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
            "if (!enabled)",
            "clearStackedRecentsTransforms();",
            "mStackedTransformsApplied = true",
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

    def test_exit_and_reset_restore_native_task_transforms(self):
        method = self.recents.split(
            "private void clearStackedRecentsTransforms()", 1
        )[1].split("    @Override", 1)[0]
        self.assertIn("if (!mStackedTransformsApplied)", method)
        self.assertIn("mStackedTransformsApplied = false;", method)
        self.assertIn("task.setStackedOverviewTransform(0f, 0f, 1f, 0f)", method)
        state = self.recents.split(
            "public void setOverviewStateEnabled(boolean enabled)", 1
        )[1].split("public void setTaskBorderEnabled", 1)[0]
        self.assertIn("if (!enabled)", state)
        self.assertIn("clearStackedRecentsTransforms();", state)
        reset = self.recents.split("public void reset()", 1)[1].split(
            "setCurrentTask(-1);", 1
        )[0]
        self.assertIn("clearStackedRecentsTransforms();", reset)

    def test_stack_thumbnail_visibility_covers_exposed_neighbors(self):
        method = self.recents.split(
            "public boolean isTaskViewVisible(TaskView tv)", 1
        )[1].split("public boolean isTaskViewFullyVisible(TaskView tv)", 1)[0]
        self.assertIn("mStackedTransformsApplied", method)
        self.assertIn("StackedRecentsGeometry.STACK_EXIT_DISTANCE", method)
        # When disabled, the original AOSP one-neighbor visibility behavior remains.
        self.assertIn(": 1;", method)

    def test_actual_thumbnail_loader_uses_stacked_window(self):
        self.assertIn("public boolean isStackedRecentsActive()", self.recents)
        loader = self.utils.split("fun getVisibleTaskIds(): List<Int>", 1)[1].split(
            "/** Returns all the TaskViews in the top row", 1
        )[0]
        self.assertIn("recentsView.isStackedRecentsActive()", loader)
        self.assertIn("StackedRecentsGeometry.STACK_EXIT_DISTANCE.toInt()", loader)
        self.assertIn("val lowerIndex = max(0, centerPageIndex - neighbors)", loader)
        self.assertIn("val upperIndex = centerPageIndex + neighbors", loader)
        self.assertIn("else {\n                        2", loader)

    def test_task_reflow_recalculates_stack_after_layout(self):
        method = self.recents.split(
            "protected void onLayout(boolean changed, int left, int top, int right, int bottom)", 1
        )[1].split("private void updatePivots()", 1)[0]
        self.assertIn("super.onLayout(changed, left, top, right, bottom)", method)
        self.assertIn("recalculateTaskViewScreenEdgeIntersections();", method)
        self.assertIn("updateCurveProperties();", method)
        self.assertLess(method.index("super.onLayout("), method.index("updateCurveProperties();"))

    def test_z_order_matches_visual_stack(self):
        self.assertIn("translationZ = depth", self.task)
        self.assertIn("translationZ = stackedDepth", self.task)
        self.assertIn("translationZ = stackedDepth", self.task.split("fun resetViewTransforms()", 1)[1])
        self.assertIn("stackedOffsetX", self.task)
        self.assertIn("stackedOffsetY", self.task)

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
