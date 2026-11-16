"""Source checks for the SettingsLib-aware Material 3 Expressive theme.

This is a static regression guard, not a substitute for a Soong build or
on-device appearance, accessibility, and gesture testing.
"""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SETTINGS = ROOT / "src/com/android/launcher3/settings"
THEME = SETTINGS / "LauncherExpressiveSettingsTheme.kt"
ACTIVITY = SETTINGS / "SettingsActivity.kt"


class LauncherExpressiveSettingsThemeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.theme = THEME.read_text(encoding="utf-8")
        cls.activity = ACTIVITY.read_text(encoding="utf-8")

    def test_settings_lib_remains_outer_theme(self):
        self.assertIn("SettingsTheme {", self.theme)
        self.assertIn("MaterialExpressiveTheme(", self.theme)
        self.assertLess(
            self.theme.index("SettingsTheme {"),
            self.theme.index("MaterialExpressiveTheme("),
        )

    def test_expressive_api_opt_in_kept_for_soong_kotlin(self):
        self.assertIn("import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi", self.theme)
        self.assertIn("@OptIn(ExperimentalMaterial3ExpressiveApi::class)", self.theme)

    def test_platform_tokens_are_forwarded(self):
        for token in (
            "colorScheme = MaterialTheme.colorScheme",
            "typography = MaterialTheme.typography",
            "shapes = MaterialTheme.shapes",
            "motionScheme = MotionScheme.expressive()",
            "content = content",
        ):
            with self.subTest(token=token):
                self.assertIn(token, self.theme)

    def test_entry_point_uses_new_theme_without_losing_route(self):
        self.assertIn(
            "LauncherExpressiveSettingsTheme { LauncherSettingsScreen(startRoute) }",
            self.activity,
        )
        self.assertIn(
            'val startRoute = intent.getStringExtra(EXTRA_START_ROUTE)',
            self.activity,
        )
        self.assertNotIn("setContent { SettingsTheme {", self.activity)

    def test_no_fixed_colors_or_forced_theme_mode(self):
        self.assertNotIn("lightColorScheme(", self.theme)
        self.assertNotIn("darkColorScheme(", self.theme)
        self.assertNotIn("Color(", self.theme)


if __name__ == "__main__":
    unittest.main()
