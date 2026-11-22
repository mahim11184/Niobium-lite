#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <cromite-repo> <chromium-src> <niobium-repo>" >&2
  exit 2
fi

CROMITE_ROOT="$(realpath "$1")"
CHROMIUM_SRC="$(realpath "$2")"
NIOBIUM_ROOT="$(realpath "$3")"
ASSETS="$NIOBIUM_ROOT/assets"

for path in \
  "$CROMITE_ROOT/build/cromite.gn_args" \
  "$CHROMIUM_SRC/chrome/android/java/AndroidManifest.xml" \
  "$CHROMIUM_SRC/chrome/android/java/res_chromium_base/values/channel_constants.xml" \
  "$CHROMIUM_SRC/tools/grit/grit/grd_reader.py" \
  "$ASSETS/icon.svg" \
  "$ASSETS/niobium-foreground.svg" \
  "$ASSETS/niobium-background.svg"; do
  [[ -f "$path" ]] || { echo "Required branding input is missing: $path" >&2; exit 1; }
done

command -v rsvg-convert >/dev/null 2>&1 || {
  echo "rsvg-convert is required. Install Ubuntu package librsvg2-bin." >&2
  exit 1
}

python3 - "$CROMITE_ROOT" "$CHROMIUM_SRC" <<'PY'
from pathlib import Path
import sys

cromite = Path(sys.argv[1])
src = Path(sys.argv[2])

def replace_once(path: Path, old: str, new: str) -> None:
    if not path.is_file():
        raise SystemExit(f"Missing branding file: {path}")
    content = path.read_text(encoding="utf-8")
    count = content.count(old)
    if count != 1:
        raise SystemExit(
            f"Expected exactly one occurrence in {path}: {old!r}; found {count}. "
            "Stopping to avoid a partial or unsafe rebrand."
        )
    path.write_text(content.replace(old, new, 1), encoding="utf-8")

def replace_all(path: Path, old: str, new: str, minimum: int = 1) -> None:
    if not path.is_file():
        raise SystemExit(f"Missing branding file: {path}")
    content = path.read_text(encoding="utf-8")
    count = content.count(old)
    if count < minimum:
        raise SystemExit(f"Expected at least {minimum} occurrence(s) of {old!r} in {path}; found {count}.")
    path.write_text(content.replace(old, new), encoding="utf-8")

# A unique package name lets Niobium Full and Niobium Lite coexist on one phone.
replace_once(
    cromite / "build/cromite.gn_args",
    'chrome_public_manifest_package = "org.cromite.cromite"',
    'chrome_public_manifest_package = "app.niobium.browser.full"',
)

# App launcher label and widget labels.
replace_once(
    src / "chrome/android/java/AndroidManifest.xml",
    'android:label="Cromite"',
    'android:label="Niobium"',
)
replace_all(
    src / "chrome/android/java/res_chromium_base/values/channel_constants.xml",
    "Cromite",
    "Niobium",
)

# The Cromite branding patch maps visible Chrome/Chromium names to Cromite.
# Change only the replacement strings; the protected technical identifiers stay intact.
grit = src / "tools/grit/grit/grd_reader.py"
for old, new in [
    ('content = content.replace("google chrome", "cromite")',
     'content = content.replace("google chrome", "niobium")'),
    ('content = content.replace("Chrome", "Cromite")',
     'content = content.replace("Chrome", "Niobium")'),
    ('content = content.replace("Chromium", "Cromite")',
     'content = content.replace("Chromium", "Niobium")'),
    ('content = content.replace("Bromite", "Cromite")',
     'content = content.replace("Bromite", "Niobium")'),
    ('content = content.replace("chrome", "cromite")',
     'content = content.replace("chrome", "niobium")'),
    ('content = content.replace("chromium", "cromite")',
     'content = content.replace("chromium", "niobium")'),
    ('content = content.replace("bromite", "cromite")',
     'content = content.replace("bromite", "niobium")'),
]:
    replace_once(grit, old, new)

# Desktop metadata isn't used by Android, but keep shared Chromium product branding consistent.
branding = src / "chrome/app/theme/chromium/BRANDING"
replace_all(branding, "Cromite", "Niobium")

# About/legal text and project help destinations.
replace_once(
    src / "chrome/browser/ui/android/strings/android_chrome_strings.grd",
    "Cromite.org",
    "Niobium Browser",
)
urls = src / "chrome/common/url_constants.h"
replace_once(
    urls,
    "https://github.com/uazo/cromite/issues",
    "https://github.com/mahim11184/Niobium/issues",
)
replace_once(
    urls,
    "https://www.cromite.org/",
    "https://github.com/mahim11184/Niobium",
)

# Never let a Niobium build silently update itself from the Cromite release channel.
updater = src / "chrome/android/java/src/org/chromium/chrome/browser/omaha/inline/BromiteInlineUpdateController.java"
replace_once(
    updater,
    "https://github.com/uazo/cromite/releases/download/",
    "https://github.com/mahim11184/Niobium/releases/download/",
)
replace_once(
    updater,
    "https://github.com/uazo/cromite/releases/latest/download/",
    "https://github.com/mahim11184/Niobium/releases/latest/download/",
)

print("Niobium text branding and Android package identity applied.")
PY

# Rebuild launcher and adaptive icon resources from the existing Niobium logo.
for spec in "mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192"; do
  density="$(printf '%s' "$spec" | cut -d: -f1)"
  size="$(printf '%s' "$spec" | cut -d: -f2)"
  dest="$CHROMIUM_SRC/chrome/android/java/res_chromium_base/mipmap-$density"
  [[ -d "$dest" ]] || { echo "Missing Android icon resource directory: $dest" >&2; exit 1; }

  rsvg-convert -w "$size" -h "$size" "$ASSETS/icon.svg" > "$dest/app_icon.png"
  rsvg-convert -w "$size" -h "$size" "$ASSETS/niobium-foreground.svg" > "$dest/layered_app_icon.png"
  rsvg-convert -w "$size" -h "$size" "$ASSETS/niobium-background.svg" > "$dest/layered_app_icon_background.png"

  # Keep the first-run welcome logo consistent with the launcher icon.
  first_run="$CHROMIUM_SRC/chrome/android/java/res_chromium/drawable-$density"
  if [[ -d "$first_run" && -f "$first_run/fre_product_logo.png" ]]; then
    rsvg-convert -w "$size" -h "$size" "$ASSETS/icon.svg" > "$first_run/fre_product_logo.png"
  fi

  browser_ui="$CHROMIUM_SRC/components/browser_ui/styles/android/java/res_chromium/drawable-$density"
  if [[ -d "$browser_ui" && -f "$browser_ui/fre_product_logo.png" ]]; then
    rsvg-convert -w "$size" -h "$size" "$ASSETS/icon.svg" > "$browser_ui/fre_product_logo.png"
  fi
done

# Android themed/monochrome icon: a clean white Niobium N.
cat > "$CHROMIUM_SRC/chrome/android/java/res_chromium_base/drawable/themed_app_icon.xml" <<'XML'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="90dp"
    android:height="90dp"
    android:viewportWidth="128"
    android:viewportHeight="128">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M31,93 L31,35 L46,35 L82,73 L82,35 L97,35 L97,93 L82,93 L46,55 L46,93 Z" />
</vector>
XML

echo "Niobium Full Android branding resources generated."
echo "App label: Niobium"
echo "Package: app.niobium.browser.full"
