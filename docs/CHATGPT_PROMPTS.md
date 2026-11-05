# ChatGPT image prompts (optional)

**You don't need any of these to launch.** Every visual in the app (the mala, lotus and yantra counters, mandala backdrop, ornaments and icon) is drawn as vector code, so it stays sharp on every screen, recolours with all 9 themes, and adds almost nothing to the app's size. The Play Store graphics are already in `store/`.

Use these prompts if you want **extra hand-crafted artwork**: a richer mandala, festival decorations, or an alternative painterly icon. They're written for ChatGPT's image generator (GPT-image). Paste the **style block** first, then one asset prompt.

---

## 1. Style block (paste at the start of every request)

```
Style: modern Hindu sacred design with sharp, precise geometry. Think temple architecture,
Sri Yantra and Bhupura gates, lotus petals with pointed tips, fine gold line-work,
Devanagari-inspired curves. Flat vector look with crisp edges, no soft blur, no photographic
texture, no 3D render, no text or letters unless asked. Colour palette: saffron #D9560B,
vermilion #B3191E, temple gold #F5C35A, pale gold #FFE7A8, deep maroon #2E0A05.
Symmetrical and centred, generous padding around the artwork.
Output: PNG with a fully TRANSPARENT background (alpha channel), no backdrop, no shadow
on the background, no border or frame around the canvas.
```

> Tip: if the result comes back on a white or checkered background, reply *"Make the background truly transparent (alpha), not white and not a checkerboard pattern."*

---

## 2. UI kit — individual transparent PNGs

Request each at **1024 × 1024** (square), except the banners.

| # | Asset | Prompt (after the style block) |
|---|---|---|
| 1 | Mandala backdrop | `A large circular mandala made only of thin gold lines: 16 pointed lotus petals, two interlaced squares forming an 8-point star, concentric rings and 12 rays. Very fine strokes, lots of empty space, suitable as a faint watermark behind a counter.` |
| 2 | Lotus divider | `A horizontal ornamental divider, 2048×256: a small sharp gold lotus in the centre with thin tapering lines extending left and right, ending in tiny diamond jewels.` |
| 3 | Corner ornament | `A single corner ornament for the top-left corner of a card: sharp temple-gate (toran) geometry in gold line-work, a 90° corner shape, empty in the middle.` |
| 4 | Om emblem | `The sacred Om symbol (ॐ) in pale-to-deep gold gradient inside a thin gold diamond frame with four tiny jewels at the points. Correct, traditional Devanagari Om shape.` |
| 5 | Rudraksha bead set | `Six individual rudraksha beads in a row, each a flat stylised vector with a carved, segmented texture in warm brown and gold highlights, evenly spaced, each separable.` |
| 6 | Tulsi mala bead set | `Six individual tulsi wood mala beads in a row, light honey-wood colour, flat vector style, slightly elongated, evenly spaced.` |
| 7 | Meru bead with tassel | `A single guru (Meru) bead larger than a normal mala bead, gold, with a sharp stylised saffron silk tassel hanging down, flat vector.` |
| 8 | Temple bell | `A hanging temple bell (ghanta) in gold, flat vector with sharp facets and a small chain on top, symmetrical.` |
| 9 | Diya lamp | `A clay diya oil lamp with a single teardrop flame, flat vector, terracotta and saffron with a gold flame outline.` |
| 10 | Kalash | `A kalash (sacred pot) with mango leaves and a coconut on top, flat vector, gold and saffron, symmetrical.` |
| 11 | Toran banner | `A horizontal festive toran garland, 2048×512: marigold flowers and mango leaves hanging in a gentle scallop, flat vector, saffron, yellow and green.` |
| 12 | Yantra frame | `A square Bhupura frame with four T-shaped temple gates on each side, double gold outline, empty centre, perfectly symmetrical.` |
| 13 | Celebration burst | `A radial burst of thin gold rays and tiny diamond sparkles around an empty centre, used for a "mala complete" celebration.` |
| 14 | Empty-state art | `A meditating figure in lotus pose drawn with a few elegant gold lines, minimal, inside a thin lotus outline.` |

### Using a PNG in the app
1. Save it as lowercase with underscores, e.g. `mandala_gold.png`, into `app/src/main/res/drawable-nodpi/`.
2. Show it in Compose:
   ```kotlin
   Image(painterResource(R.drawable.mandala_gold), contentDescription = null, modifier = Modifier.size(320.dp).alpha(0.08f))
   ```
   For example, replace `MandalaBackdrop(...)` in `ui/counter/CounterScreen.kt` with this to use a painted mandala behind the counter.
3. Keep PNGs under about 300 KB each (use <https://tinypng.com>, or convert to WebP in Android Studio: right-click → *Convert to WebP*).

> Note: raster art won't recolour with the 9 themes the way the built-in vector art does. Gold or neutral artwork works best across all themes.

---

## 3. Alternative app icon (optional)

The current icon (gold mala + Om on saffron-maroon) is generated in `tools/assets.py`. For a painterly alternative:

```
[style block, but replace the last paragraph with:]
Output: a square 1024×1024 PNG app icon, full-bleed background (NOT transparent).
Subject: a circular japa mala of gold beads with one larger Meru bead and a small tassel at
the top, the Om symbol (ॐ) in luminous pale gold at the centre, on a rich saffron-to-deep-maroon
radial gradient. Keep all important elements inside the central 66% circle (Android masks icons
into circles and squircles). No text, no rounded corners (the store adds them), no drop shadow
outside the art.
```
Then upload to Play as `icon-512.png` (resize to 512×512). For the launcher, use Android Studio's *Image Asset* tool (right-click `res` → *New → Image Asset*) with a transparent version of the foreground.

## 4. Alternative feature graphic (optional)

```
Create a 1024×500 Google Play feature graphic, full-bleed (NOT transparent), no alpha.
Left third: a glowing gold japa mala (108 beads, Meru bead and tassel) circling the Om symbol (ॐ).
Right two-thirds: leave a calm, uncluttered saffron-to-maroon gradient area with faint gold
mandala lines. I will add the text myself. Do not render any text. Keep key artwork away from
the outer 5% edges.
```
Add the title "MANTRA MALA · 108 JAPA COUNTER" in Canva or Figma. Avoid words like "best", "#1" or "free", which Play rejects.

## 5. Social / promo square (optional)

```
[style block, full-bleed instead of transparent]
1080×1080 Instagram post: a phone mock-up (generic, no brand logos) showing a saffron japa
counter with a gold mala ring and the number 108 in the centre, floating above a soft mandala
backdrop, a few marigold petals around. No text.
```
