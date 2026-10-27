#!/usr/bin/env bash
# Copies the game into the app so it runs fully offline inside the APK. Run from the android-app folder:
#   ./copy_game.sh [path-to-game-folder]      (default: the parent folder)
# If the game files are not there, nothing is copied and you get a small "viewer" APK that asks for the game address.
SRC="${1:-..}"
DST="app/src/main/assets/game"
rm -rf "$DST"
if [ ! -f "$SRC/index.html" ] || [ ! -d "$SRC/Assets" ]; then
  echo "No game found in $SRC: building the small VIEWER app (it asks for the game address on first start)."
  exit 0
fi
mkdir -p "$DST"
for f in index.html NIGHTMARE.html gameover.html admin.html GameEditor.html style.css nightmare.css hdb.css hdb.js settings.js features.js game.js nightmare.js tutorial.js nightmare-tutorial.js manifest.webmanifest sw.js; do
  [ -f "$SRC/$f" ] && cp "$SRC/$f" "$DST/"
done
cp -r "$SRC/Assets" "$DST/Assets"; [ -d "$SRC/icons" ] && cp -r "$SRC/icons" "$DST/icons"
echo "Game copied to $DST ($(du -sh "$DST" | cut -f1))"
