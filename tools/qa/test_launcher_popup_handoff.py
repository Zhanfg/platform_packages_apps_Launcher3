"""Regression guard for the Launcher3 popup / All Apps transition.

This verifies source-level invariants only. It is not an Android build or device test.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
LAUNCHER = ROOT / "src/com/android/launcher3/Launcher.java"
ALL_APPS = ROOT / "quickstep/src/com/android/launcher3/uioverrides/states/AllAppsState.java"
ARROW_POPUP = ROOT / "src/com/android/launcher3/popup/ArrowPopup.java"


class PopupHandoffTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.launcher = LAUNCHER.read_text(encoding="utf-8")
        cls.all_apps = ALL_APPS.read_text(encoding="utf-8")
        cls.arrow_popup = ARROW_POPUP.read_text(encoding="utf-8")

    def test_handoff_before_base_state_start(self):
        signature = "public void onStateSetStart(LauncherState state)"
        self.assertEqual(self.launcher.count(signature), 1)
        method = self.launcher.split(signature, 1)[1].split(
            "super.onStateSetStart(state);", 1
        )[0]
        self.assertRegex(method, r"if\s*\(state\s*==\s*ALL_APPS\)")
        self.assertIn(
            "AbstractFloatingView.closeOpenViews(this, false, TYPE_OPTIONS_POPUP);",
            method,
        )
        self.assertEqual(method.count("closeOpenViews("), 1)

    def test_only_options_popup_is_removed(self):
        self.assertIn(
            "import static com.android.launcher3.AbstractFloatingView.TYPE_OPTIONS_POPUP;",
            self.launcher,
        )
        self.assertIn("TYPE_OPTIONS_POPUP = 1 << 12", (
            ROOT / "src/com/android/launcher3/AbstractFloatingView.java"
        ).read_text(encoding="utf-8"))
        self.assertNotIn(
            "AbstractFloatingView.closeAllOpenViews(this, false);",
            self.launcher,
        )

    def test_all_apps_state_still_closes_popups(self):
        self.assertRegex(
            self.all_apps,
            r"FLAG_WORKSPACE_INACCESSIBLE\s*\|\s*FLAG_CLOSE_POPUPS",
        )

    def test_popup_has_synchronous_cleanup_path(self):
        self.assertRegex(
            self.arrow_popup,
            r"protected void handleClose\(boolean animate\)\s*\{"
            r"\s*if \(animate\)\s*\{\s*animateClose\(\);"
            r"\s*\}\s*else\s*\{\s*closeComplete\(\);",
        )


if __name__ == "__main__":
    unittest.main()
