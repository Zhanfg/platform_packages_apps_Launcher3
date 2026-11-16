"""Material 3 Expressive consistency guards across launcher settings and view surfaces.

Source and XML tests only: Android 17 Soong compilation and device appearance still required.
"""
from pathlib import Path
import unittest
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[2]
SETTINGS = ROOT / "src/com/android/launcher3/settings/LauncherSettingsScreen.kt"
PALETTE = ROOT / "src/com/android/launcher3/qsb/LauncherExpressiveSearchColors.kt"
QSB_NATIVE = ROOT / "src/com/android/launcher3/qsb/SearchBarUI.kt"
QSB_COMPOSE = ROOT / "src/com/android/launcher3/qsb/LawnQsbUi.kt"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class ExpressiveSurfaceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.settings = SETTINGS.read_text(encoding="utf-8")
        cls.palette = PALETTE.read_text(encoding="utf-8")
        cls.qsb_native = QSB_NATIVE.read_text(encoding="utf-8")
        cls.qsb_compose = QSB_COMPOSE.read_text(encoding="utf-8")

    def test_native_and_compose_search_share_dynamic_surface(self):
        self.assertIn("R.color.materialColorSurfaceContainer", self.palette)
        self.assertIn("LauncherExpressiveSearchColors.container(context)", self.qsb_native)
        self.assertIn("LauncherExpressiveSearchColors.container(context)", self.qsb_compose)
        self.assertIn("HOTSEAT_QSB_THEMED", self.qsb_native)
        # Explicit custom settings must remain untouched by the themed branch.
        self.assertIn("else backgroundColor", self.qsb_compose)

    def test_qsb_action_icons_use_expressive_size_and_reduced_motion(self):
        self.assertIn("SizeTransform(clip = false)", self.qsb_compose)
        self.assertIn("Spring.DampingRatioLowBouncy", self.qsb_compose)
        self.assertIn("ValueAnimator.areAnimatorsEnabled()", self.qsb_compose)
        self.assertIn("tween(0)", self.qsb_compose)

    def test_settings_search_is_expressive_and_focus_aware(self):
        src = self.settings.split("private fun SettingsSearchBar(", 1)[1].split(
            "private data class SettingsSearchResult", 1
        )[0]
        for token in (
            "animateDpAsState(",
            "animateColorAsState(",
            "RoundedCornerShape(radius)",
            "onFocusChanged { focused = it.isFocused }",
            "MaterialTheme.colorScheme.primaryContainer",
            "MaterialTheme.colorScheme.surfaceContainerLow",
            "Box(contentAlignment = Alignment.CenterStart)",
            "ValueAnimator.areAnimatorsEnabled()",
        ):
            with self.subTest(token=token):
                self.assertIn(token, src)

    def test_expressive_reset_and_icons(self):
        self.assertIn("FilledTonalIconButton(onClick = onClick)", self.settings)
        self.assertIn("modifier = Modifier.size(48.dp)", self.settings)
        self.assertIn("shape = MaterialTheme.shapes.large", self.settings)

    def test_native_drawable_surface_shapes(self):
        pairs = [
            ("bg_all_apps_searchbox.xml", "solid", "color",
             "@color/materialColorSurfaceContainer"),
            ("round_rect_primary.xml", "solid", "color",
             "@color/materialColorPrimary"),
            ("round_rect_folder.xml", "corners", "radius",
             "@dimen/launcher_expressive_folder_radius"),
        ]
        for filename, tag, attribute, token in pairs:
            with self.subTest(file=filename):
                root = ElementTree.parse(ROOT / "res/drawable" / filename).getroot()
                self.assertEqual(root.find(tag).get(ANDROID + attribute), token)

        # Android 17 actually selects drawable-v28 overrides for these names.
        for qualifier in ("drawable", "drawable-v28"):
            for name, tag, attr, expected in (
                ("round_rect_folder.xml", "corners", "radius",
                 "@dimen/launcher_expressive_folder_radius"),
                ("round_rect_primary.xml", "solid", "color",
                 "@color/materialColorPrimary"),
            ):
                with self.subTest(qualifier=qualifier, resource=name):
                    root = ElementTree.parse(ROOT / "res" / qualifier / name).getroot()
                    self.assertEqual(root.find(tag).get(ANDROID + attr), expected)

        top = ElementTree.parse(
            ROOT / "res/drawable-v28/top_round_rect_primary.xml"
        ).getroot()
        self.assertEqual(top.find("solid").get(ANDROID + "color"),
                         "@color/materialColorPrimary")

        dims = ElementTree.parse(ROOT / "res/values/dimens.xml").getroot()
        dim = next(d for d in dims.findall("dimen")
                   if d.get("name") == "launcher_expressive_folder_radius")
        self.assertEqual(dim.text.strip(), "24dp")


if __name__ == "__main__":
    unittest.main()
