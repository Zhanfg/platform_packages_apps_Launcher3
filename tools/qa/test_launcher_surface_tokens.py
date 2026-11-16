"""Source-level Material color / shape consistency checks.

These tests do not replace resource linking or on-device contrast QA.
"""
from pathlib import Path
import unittest
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"


class LauncherSurfaceTokenTests(unittest.TestCase):
    def test_edge_effect_uses_dynamic_primary_in_all_qualifiers(self):
        for resource_dir in ("values", "values-v29", "values-v30"):
            with self.subTest(qualifier=resource_dir):
                tree = ET.parse(ROOT / "res" / resource_dir / "styles.xml")
                theme = next(
                    item
                    for item in tree.getroot().findall("style")
                    if item.get("name") == "BaseLauncherTheme"
                )
                edge = next(
                    item for item in theme.findall("item")
                    if item.get("name") == "android:colorEdgeEffect"
                )
                self.assertEqual(edge.text.strip(), "@color/materialColorPrimary")

    def test_tooltip_surface_and_text_follow_material_tokens(self):
        frame = ET.parse(ROOT / "res/drawable/tooltip_frame.xml").getroot()
        label = ET.parse(ROOT / "res/layout/drop_target_tool_tip.xml").getroot()
        self.assertEqual(
            frame.find("solid").attrib[ANDROID + "color"],
            "@color/materialColorSurfaceContainer",
        )
        self.assertEqual(
            frame.find("corners").attrib[ANDROID + "radius"],
            "@dimen/popup_smaller_radius",
        )
        self.assertEqual(
            label.attrib[ANDROID + "textColor"],
            "@color/materialColorOnSurface",
        )

    def test_reference_tokens_already_used_by_launcher(self):
        src = (ROOT / "res/drawable/work_mode_fab_background.xml").read_text()
        self.assertIn("@color/materialColorPrimary", src)
        popup = (ROOT / "res/drawable/popup_background.xml").read_text()
        self.assertIn("@color/materialColorSurfaceContainer", popup)


if __name__ == "__main__":
    unittest.main()
