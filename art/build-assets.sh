#!/usr/bin/env bash
# Builds the RGBTv launcher icon, TV banner and store feature graphic from the source renders.
#
# The AI-generated banner/feature renders are used as *blurred backdrop glow* only: that keeps
# their neon colour and depth while removing any risk of shipping garbled AI lettering. The
# wordmark is drawn here with a real font so the brand text is always crisp and correct.
#
# Requires ImageMagick 7 (`convert`).
set -euo pipefail
cd "$(dirname "$0")"

RES=../app/src/main/res
FONT=DejaVu-Sans-Bold
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "→ normalising sources"
convert source-icon.png -strip -resize 1024x1024! source-icon.png
convert source-banner.png -strip -resize 1280x720! source-banner.png
convert source-feature.png -strip -resize 1024x500! source-feature.png
convert source-icon.png -strip -resize 512x512! icon-512.png

echo "→ launcher foreground (432x432 = 108dp @ xxxhdpi)"
mkdir -p "$RES/mipmap-anydpi-v26"
convert source-icon.png -strip -resize 432x432! "$RES/mipmap-anydpi-v26/ic_launcher_foreground.png"

echo "→ TV banner (320x180dp)"
mkdir -p "$RES/drawable-xhdpi" "$RES/drawable-xxhdpi"
W=960; H=540; ICON=300
convert source-banner.png -resize "${W}x${H}!" -blur 0x40 -brightness-contrast -30x0 -fill black -colorize 20% "$TMP/bg.png"
convert icon-1024.png -resize "${ICON}x${ICON}!" "$TMP/mark.png"
Y=$(( (H - ICON) / 2 ))
convert "$TMP/bg.png" "$TMP/mark.png" -geometry "+90+${Y}" -composite "$TMP/b1.png"
PS=150
X=$(( 90 + ICON + 60 ))
BASE=$(( H / 2 + PS / 3 ))
RGBW=$(convert -font "$FONT" -pointsize "$PS" label:RGB -format "%w" info:)
convert "$TMP/b1.png" \
  -font "$FONT" -pointsize "$PS" -fill "#F6F7FB" -annotate "+${X}+${BASE}" "RGB" \
  -font "$FONT" -pointsize "$PS" -fill "#FF3D71" -annotate "+$(( X + RGBW ))+${BASE}" "Tv" \
  "$TMP/banner.png"
convert "$TMP/banner.png" -strip -resize 960x540! "$RES/drawable-xxhdpi/tv_banner.png"
convert "$TMP/banner.png" -strip -resize 640x360! "$RES/drawable-xhdpi/tv_banner.png"

echo "→ store feature graphic (1024x500)"
FW=1024; FH=500; FICON=220
convert source-feature.png -resize "${FW}x${FH}!" -blur 0x40 -brightness-contrast -30x0 -fill black -colorize 25% "$TMP/fbg.png"
convert icon-1024.png -resize "${FICON}x${FICON}!" "$TMP/fmark.png"
FY=$(( (FH - FICON) / 2 ))
FX=110
convert "$TMP/fbg.png" "$TMP/fmark.png" -geometry "+${FX}+${FY}" -composite "$TMP/f1.png"
FPS=110
TX=$(( FX + FICON + 55 ))
TBASE=$(( FH / 2 + FPS / 3 ))
FRGBW=$(convert -font "$FONT" -pointsize "$FPS" label:RGB -format "%w" info:)
convert "$TMP/f1.png" \
  -font "$FONT" -pointsize "$FPS" -fill "#F6F7FB" -annotate "+${TX}+${TBASE}" "RGB" \
  -font "$FONT" -pointsize "$FPS" -fill "#FF3D71" -annotate "+$(( TX + FRGBW ))+${TBASE}" "Tv" \
  -font "$FONT" -pointsize 34 -fill "#20D9D2" -annotate "+${TX}+$(( TBASE + 60 ))" "Native IPTV player" \
  -strip feature-graphic-1024x500.png

echo "→ optimising"
for f in "$RES/mipmap-anydpi-v26/ic_launcher_foreground.png" \
         "$RES/drawable-xhdpi/tv_banner.png" \
         "$RES/drawable-xxhdpi/tv_banner.png" \
         icon-512.png feature-final.png; do
  convert "$f" -strip -define png:compression-level=9 "$f"
done

echo "→ removing superseded vector banner"
rm -f "$RES/drawable/tv_banner.xml"

echo "done"; ls -l "$RES/mipmap-anydpi-v26" "$RES/drawable-xhdpi" "$RES/drawable-xxhdpi"
