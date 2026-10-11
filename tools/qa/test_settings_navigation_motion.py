"""Static regression check for Launcher Settings navigation motion.

Source-level guard only; Android module build and device tests are separate.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "src/com/android/launcher3/settings/LauncherSettingsScreen.kt"


class SettingsNavigationMotionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SOURCE.read_text(encoding="utf-8")
        cls.host = cls.source.split("        NavHost(", 1)[1].split("        ) {", 1)[0]

    def test_all_navigation_directions_present(self):
        for name in ("enterTransition", "exitTransition", "popEnterTransition", "popExitTransition"):
            self.assertEqual(self.host.count(f"{name} = {{"), 1)

    def test_fade_and_slide_follow_same_clock(self):
        self.assertIn("private const val SETTINGS_NAV_DURATION_MS = 300", self.source)
        self.assertEqual(self.host.count("animationSpec = tween(SETTINGS_NAV_DURATION_MS)"), 4)
        self.assertEqual(self.host.count("fadeIn(tween(SETTINGS_NAV_DURATION_MS))"), 2)
        self.assertEqual(self.host.count("fadeOut(tween(SETTINGS_NAV_DURATION_MS))"), 2)

    def test_no_phase_offset_in_route_transitions(self):
        self.assertNotIn("delayMillis", self.host)
        self.assertNotRegex(self.host, r"fade(?:In|Out)\(tween\(\d+")

    def test_slide_direction_is_unchanged(self):
        self.assertEqual(self.host.count("SlideDirection.Start"), 2)
        self.assertEqual(self.host.count("SlideDirection.End"), 2)
        self.assertEqual(self.host.count("initialOffset = { it / 5 }"), 2)
        self.assertEqual(self.host.count("targetOffset = { it / 5 }"), 2)


if __name__ == "__main__":
    unittest.main()
