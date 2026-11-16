#!/usr/bin/env python3
"""Merge uwuAOSP Launcher3 and Quickstep resource overlays for a Gradle-only probe.

Soong knows overlay precedence. Gradle's peer res.srcDirs do NOT: it reports
duplicate resource definitions for the same qualifier. This staging-only
tool applies the explicit precedence below and leaves the fork unchanged.

Precedence: adapter-generated resources < uwu Launcher3 res < uwu Quickstep res.
"""
from __future__ import annotations

from collections import OrderedDict
from pathlib import Path
from shutil import copy2
import sys
import xml.etree.ElementTree as ET


def resource_key(element: ET.Element, index: int):
    name = element.attrib.get("name")
    if name is None:
        return ("unnamed", index)
    tag = element.tag.rsplit("}", 1)[-1]
    return (element.attrib.get("type", tag) if tag == "item" else tag, name)


def merge(adapter: Path, fork: Path):
    output = adapter / "res_merged"
    sources = (output, fork / "res", fork / "quickstep/res")
    values: dict[str, OrderedDict] = {}
    unnamed_counter = 0
    conflicts = 0
    files = 0

    for source in sources:
        if not source.is_dir():
            raise SystemExit(f"Missing Android resource source: {source}")
        for src in sorted(source.rglob("*")):
            if not src.is_file():
                continue
            rel = src.relative_to(source)
            qualifier = rel.parts[0]
            if qualifier.startswith("values") and src.suffix == ".xml":
                root = ET.parse(src).getroot()
                if root.tag != "resources":
                    raise ValueError(f"Unexpected resource root in {src}")
                items = values.setdefault(qualifier, OrderedDict())
                for elem in root:
                    key = resource_key(elem, unnamed_counter)
                    unnamed_counter += 1
                    if key in items:
                        conflicts += 1
                    # Last overlay is authoritative, including for styles and arrays.
                    items[key] = elem
            else:
                dst = output / rel
                # Never copy the same file onto itself when source is output.
                if source != output:
                    dst.parent.mkdir(parents=True, exist_ok=True)
                    copy2(src, dst)
                    files += 1

    for qualifier, contents in sorted(values.items()):
        dst = output / qualifier
        dst.mkdir(parents=True, exist_ok=True)
        for existing in dst.glob("*.xml"):
            existing.unlink()
        root = ET.Element("resources")
        root.extend(contents.values())
        ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
        ET.register_namespace("tools", "http://schemas.android.com/tools")
        ET.register_namespace("xliff", "urn:oasis:names:tc:xliff:document:1.2")
        ET.ElementTree(root).write(dst / "uwu_gradle_merged.xml",
                                   encoding="utf-8", xml_declaration=True)

    print(f"Merged {len(values)} value qualifiers, {files} non-value overlays, "
          f"{conflicts} replaced resource definitions")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: merge_android17_resources.py ADAPTER_ROOT FORK_ROOT")
    merge(Path(sys.argv[1]).resolve(), Path(sys.argv[2]).resolve())
