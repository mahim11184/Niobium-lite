#!/usr/bin/env bash
set -euo pipefail
SRC="${1:?usage: rebrand-ungoogled.sh <chromium-src> <work-root> <niobium-root>}"
WORK="${2:?missing work root}"
NIOBIUM="${3:?missing Niobium checkout}"
ICON="$NIOBIUM/assets/icon.svg"
FG="$NIOBIUM/assets/niobium-foreground.svg"
BG="$NIOBIUM/assets/niobium-background.svg"
test -s "$ICON"
test -s "$FG"
test -s "$BG"

# Keep Lite and Full installable side by side. GN args are outside the source
# fingerprint, so this overlay remains stable across hosted build stages.
ARGS="$SRC/out/Default/args.gn"
mkdir -p "$(dirname "$ARGS")"
grep -q '^chrome_public_manifest_package[[:space:]]*=' "$ARGS" \
  && sed -i 's/^chrome_public_manifest_package[[:space:]]*=.*/chrome_public_manifest_package = "app.niobium.browser.full"/' "$ARGS" \
  || printf '\nchrome_public_manifest_package = "app.niobium.browser.full"\n' >> "$ARGS"

python3 - "$SRC" <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
import sys
src = Path(sys.argv[1])
changed = 0
# Only change Android UI string resources with app-name identifiers; do not
# globally replace Chromium in technical/legal identifiers.
for p in src.glob("chrome/android/java/res*/values*/strings.xml"):
    try:
        tree = ET.parse(p)
    except (ET.ParseError, OSError):
        continue
    root = tree.getroot()
    dirty = False
    for node in root.findall("string"):
        if node.get("name") in {"app_name", "app_name_short", "application_name"}:
            node.text = "Niobium"
            dirty = True
            changed += 1
    if dirty:
        tree.write(p, encoding="utf-8", xml_declaration=True)
if changed == 0:
    raise SystemExit("Could not find Android app-name string resource; refusing a partial rebrand")
print(f"Updated {changed} Android app-name resource(s)")
PY

if ! command -v rsvg-convert >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y librsvg2-bin
fi
for d in "$SRC"/chrome/android/java/res_chromium_base/mipmap-*; do
  [ -d "$d" ] || continue
  case "$d" in
    *mipmap-mdpi) size=48 ;;
    *mipmap-hdpi) size=72 ;;
    *mipmap-xhdpi) size=96 ;;
    *mipmap-xxhdpi) size=144 ;;
    *mipmap-xxxhdpi) size=192 ;;
    *) continue ;;
  esac
  rsvg-convert -w "$size" -h "$size" "$ICON" > "$d/app_icon.png"
  rsvg-convert -w "$size" -h "$size" "$FG" > "$d/layered_app_icon.png"
  rsvg-convert -w "$size" -h "$size" "$BG" > "$d/layered_app_icon_background.png"
done

cat > "$WORK/NIOBIUM-BUILD-IDENTITY.txt" <<'EOF'
Product: Niobium Full
Base: ungoogled-chromium-android
Package: app.niobium.browser.full
Lite package: app.niobium.browser
Architecture: arm64
EOF
echo "Niobium Full package identity, app label and launcher artwork applied."
