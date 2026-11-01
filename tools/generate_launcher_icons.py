#!/usr/bin/env python3
"""Generate Android VectorDrawable resources from the checked-in SVG paths."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
svg = ET.parse(root / 'artwork/paradee-parakeet.svg').getroot()
paths = list(svg.findall('{http://www.w3.org/2000/svg}path'))
assert len(paths) > 1 and paths[0].get('data-layer') == 'background'
assert paths[0].get('fill') == '#123B36'


def make_vector(items):
    lines = [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp" android:height="108dp"',
        '    android:viewportWidth="108" android:viewportHeight="108">',
    ]
    for item in items:
        attrs = [f'android:pathData="{item.attrib["d"]}"']
        fill = item.get('fill')
        attrs.append(f'android:fillColor="{fill if fill and fill != "none" else "@android:color/transparent"}"')
        if item.get('stroke'):
            attrs += [f'android:strokeColor="{item.attrib["stroke"]}"',
                      f'android:strokeWidth="{item.attrib["stroke-width"]}"']
            if item.get('stroke-linecap'):
                attrs.append(f'android:strokeLineCap="{item.attrib["stroke-linecap"]}"')
        lines.append('    <path ' + ' '.join(attrs) + '/>')
    lines.append('</vector>')
    return '\n'.join(lines) + '\n'

files = {
    'app/src/main/res/drawable/ic_parakeet_foreground.xml': make_vector(paths[1:]),
    'app/src/main/res/mipmap-anydpi-v21/ic_launcher.xml': make_vector(paths),
    'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml': '''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/parakeet_icon_background"/>\n    <foreground android:drawable="@drawable/ic_parakeet_foreground"/>\n</adaptive-icon>\n''',
    'app/src/main/res/values/icon_colors.xml': '''<resources>\n    <color name="parakeet_icon_background">#123B36</color>\n</resources>\n''',
}
for name, contents in files.items():
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(contents, encoding='utf-8')
    print(path.relative_to(root))
