"""Composes framed 1080x1920 Play Store screenshots (16:9, within Play's 2:1 limit) from raw device captures.
Usage (from the repo root):  python tools/store_screens.py <raw_captures_dir> store/screenshots
Raw captures are full-resolution `adb exec-out screencap -p` PNGs named as in SLIDES below."""
import base64, os, subprocess, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
RAW, OUT = sys.argv[1:3]
FONTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app/src/main/res/font")
os.makedirs(OUT, exist_ok=True)

import uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
CINZEL = os.path.join(FONTS, "cinzel.ttf")

def n(v): return ("%.2f" % v).rstrip("0").rstrip(".")
def text_path(font_path, text, size, wght=700, tracking=2.0):
    blob = hb.Blob.from_file_path(font_path); face = hb.Face(blob); font = hb.Font(face)
    font.set_variations({"wght": wght})
    buf = hb.Buffer(); buf.add_str(text); buf.guess_segment_properties(); hb.shape(font, buf, {})
    tt = TTFont(font_path); gs = tt.getGlyphSet(location={"wght": wght}); order = tt.getGlyphOrder()
    s = size / face.upem; x = 0.0; parts = []
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        pen = SVGPathPen(gs, ntos=n)
        gs[order[info.codepoint]].draw(TransformPen(pen, (s, 0, 0, -s, x + pos.x_offset * s, -pos.y_offset * s)))
        parts.append(pen.getCommands()); x += pos.x_advance * s + tracking
    return "".join(parts), x - tracking

SLIDES = [
    ("a_japa_light.png", "COUNT EVERY BEAD", ["Tap anywhere on the mala — 108 beads,", "a gentle vibration, a bell at completion"]),
    ("b_japa_dark.png", "CHANT BEFORE DAWN", ["Night mode and the Yantra counter", "for your Brahma Muhurta sadhana"]),
    ("c_mantras_light.png", "14 SACRED MANTRAS", ["Sanskrit, transliteration and meaning —", "or add the mantra your guru gave you"]),
    ("d_history_light.png", "YOUR SADHANA, KEPT", ["Your lifetime record from day one —", "streaks, goals and every mala, on your phone"]),
    ("e_themes_dark.png", "MAKE IT YOURS", ["9 sacred colour themes · 3 counter styles", "Day and night, one tap away"]),
    ("f_hindi_lotus.png", "YOUR LANGUAGE", ["हिन्दी · English · Русский · Español · Deutsch", "Français · 中文 · 日本語 · 한국어"]),
    ("g_overlay_home.png", "COUNT FROM ANYWHERE", ["A floating bead over any app —", "move it, resize it, colour it your way"]),
]

W, H = 1080, 1920
SW = 690                     # screenshot width inside the frame
for i, (raw, title, sub) in enumerate(SLIDES, 1):
    shot = Image.open(os.path.join(RAW, raw)).convert("RGB")
    sh = round(shot.height * SW / shot.width)
    shot = shot.resize((SW, sh), Image.LANCZOS)
    tmp = os.path.join(OUT, f".tmp_{i}.png"); shot.save(tmp)
    b64 = base64.b64encode(open(tmp, "rb").read()).decode()
    tp, tw = text_path(CINZEL, title, 66, tracking=3)
    tw_scale = min(1.0, 960 / tw)
    x0 = (W - SW) / 2; y0 = 392
    sub_svg = "".join(
        f'<text x="540" y="{290 + k * 46}" text-anchor="middle" font-family="Helvetica Neue, PingFang SC, Hiragino Sans, Apple SD Gothic Neo, Kohinoor Devanagari, sans-serif" '
        f'font-size="34" fill="#FFE7A8" fill-opacity="0.88">{line}</text>' for k, line in enumerate(sub)
    )
    rays = "".join(f'<line x1="540" y1="-40" x2="{540 + 1400 * __import__("math").cos(__import__("math").radians(a))}" '
                   f'y2="{-40 + 1400 * __import__("math").sin(__import__("math").radians(a))}"/>' for a in range(15, 180, 15))
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <radialGradient id="bg" gradientUnits="userSpaceOnUse" cx="540" cy="160" r="1900">
    <stop offset="0" stop-color="#B8441A"/><stop offset="0.35" stop-color="#6E1B09"/><stop offset="1" stop-color="#1E0604"/>
  </radialGradient>
  <linearGradient id="tg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF4D6"/><stop offset="1" stop-color="#F5C35A"/></linearGradient>
  <clipPath id="scr"><rect x="{x0}" y="{y0}" width="{SW}" height="{sh}" rx="40"/></clipPath>
</defs>
<rect width="{W}" height="{H}" fill="url(#bg)"/>
<g stroke="#F5C35A" stroke-opacity="0.07" stroke-width="2">{rays}</g>
<g fill="none" stroke="#F5C35A" stroke-opacity="0.09" stroke-width="2"><circle cx="540" cy="-40" r="420"/><circle cx="540" cy="-40" r="560"/></g>
<g transform="translate({n((W - tw * tw_scale) / 2)},172) scale({n(tw_scale)})"><path d="{tp}" fill="url(#tg)"/></g>
<g fill="#F5C35A"><rect x="400" y="209" width="122" height="2" opacity="0.7"/><path d="M540,202L548,210L540,218L532,210Z"/><rect x="558" y="209" width="122" height="2" opacity="0.7"/></g>
{sub_svg}
<rect x="{x0 - 14}" y="{y0 - 14}" width="{SW + 28}" height="{sh + 28}" rx="52" fill="#120402" stroke="#F5C35A" stroke-opacity="0.55" stroke-width="2"/>
<image x="{x0}" y="{y0}" width="{SW}" height="{sh}" clip-path="url(#scr)" xlink:href="data:image/png;base64,{b64}"/>
</svg>'''
    svg_path = os.path.join(OUT, f".tmp_{i}.svg"); open(svg_path, "w").write(svg)
    png = os.path.join(OUT, f"screenshot-{i:02d}-{raw.split('.')[0][2:].replace('_', '-')}.png")
    subprocess.run(["rsvg-convert", "-w", str(W), "-h", str(H), "-o", png, svg_path], check=True)
    Image.open(png).convert("RGB").save(png, optimize=True)  # Play requires no alpha
    os.remove(svg_path); os.remove(tmp)
    print(png)
