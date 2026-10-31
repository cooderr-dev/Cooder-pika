package com.example.wllpr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** A 10-step colour ramp, darkest to lightest. */
class Palette(val name: String, hex: List<String>) {
    val colors: IntArray = IntArray(hex.size) { Color.parseColor(hex[it]) }
}

object Palettes {
    val all: List<Palette> = listOf(
        Palette("Charcoal", listOf("#000000", "#141414", "#2a2a2a", "#404040", "#595959", "#737373", "#8c8c8c", "#b3b3b3", "#d9d9d9", "#ffffff")),
        Palette("Stone", listOf("#1a1714", "#2c2825", "#3e3a35", "#534e47", "#6b655c", "#857d72", "#9f9688", "#b8b0a3", "#d1c9bd", "#ece6dc")),
        Palette("Blue", listOf("#01052b", "#04137a", "#0825c7", "#0d39ff", "#1552ff", "#2f73ff", "#58a3ff", "#7dc3ff", "#aedfff", "#edf8ff")),
        Palette("Sunrise", listOf("#031c35", "#05284a", "#063862", "#0b4b7a", "#ff5b40", "#ff7240", "#ff9438", "#ffb33f", "#ffd064", "#fff0c8")),
        Palette("Fire", listOf("#1a0000", "#450000", "#7a0000", "#b30000", "#e63900", "#ff6a00", "#ff9500", "#ffb700", "#ffd166", "#fff1c2")),
        Palette("Purple", listOf("#100014", "#22002e", "#3f005a", "#5c0085", "#7a00b3", "#9b1fff", "#b84dff", "#cf88ff", "#e4c2ff", "#f7ebff")),
        Palette("Toxic Glow", listOf("#050807", "#0c1410", "#14221a", "#1e3325", "#2a4d2e", "#3f6b2f", "#5f8a2c", "#86b326", "#b9e83f", "#f6ffe0")),
        Palette("Arctic Winter", listOf("#081018", "#112131", "#1d3348", "#33516a", "#55768f", "#7d9eb5", "#a7c2d3", "#cfe0ea", "#e8f0f5", "#ffffff")),
        Palette("Neon Horizon", listOf("#13051a", "#290d3a", "#3c1259", "#3b2285", "#2b42b0", "#1d6cd4", "#2ca1e8", "#59cef2", "#94f2f7", "#e0fbfd")),
        Palette("Solar Flare", listOf("#1f0505", "#3a0d0d", "#611111", "#871c26", "#ab3037", "#cb4f41", "#e6744a", "#f79d5c", "#fcc579", "#fdf2a6")),
        Palette("Deep Forest", listOf("#051214", "#0b2426", "#123a39", "#18524c", "#226b5d", "#32856c", "#49a07a", "#68ba89", "#91d49d", "#c3ebd0")),
        Palette("Cosmic Blush", listOf("#120414", "#260824", "#3f0f3d", "#5c1a5c", "#7a2a7a", "#a33a86", "#c85b98", "#e98fb6", "#ffd0dd", "#fff1f6")),
        Palette("Ocean Depths", listOf("#020c1b", "#0a1628", "#0f2847", "#0d3b6e", "#0e5a8a", "#1a7fa0", "#30a5b8", "#5eccc5", "#96ead8", "#d4fff2")),
        Palette("Blood Moon", listOf("#000000", "#0f0000", "#1f0000", "#330000", "#4d0000", "#6b0000", "#880000", "#a50000", "#c40000", "#e60000")),
    )
}

val PATTERN_LABELS = listOf("Hills", "Waves", "Dunes", "Mountains", "Arcs", "Scribble")

/** Everything needed to reproduce one wallpaper deterministically. */
data class Spec(val pattern: Int, val palette: Int, val seed: Int, val inverted: Boolean) {
    fun key(w: Int, h: Int) = "$pattern-$palette-$seed-$inverted-${w}x$h"
}

/** Background colour of a spec: first ramp colour (dark mode) or last (light mode). */
fun backgroundColor(spec: Spec): Int {
    val c = Palettes.all[spec.palette].colors
    return if (spec.inverted) c.last() else c.first()
}

fun isLightBackground(spec: Spec): Boolean {
    val bg = backgroundColor(spec)
    val brightness = (Color.red(bg) * 299 + Color.green(bg) * 587 + Color.blue(bg) * 114) / 1000
    return brightness > 125
}

object Engine {
    /** Renders a wallpaper into a new bitmap. Safe to call from any background thread. */
    fun render(w: Int, h: Int, spec: Spec): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val painter = Painter(Canvas(bmp), w, h, Palettes.all[spec.palette], spec.seed, spec.inverted)
        painter.draw(spec.pattern)
        return bmp
    }
}

/** Small, fast, seedable PRNG (mulberry32). Returns values in [0, 1). */
private class Mulberry32(seed: Int) {
    private var a = seed

    operator fun invoke(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

private class Painter(
    private val c: Canvas,
    wi: Int,
    hi: Int,
    private val pal: Palette,
    private val seed: Int,
    private val inv: Boolean,
) {
    private val w = wi.toDouble()
    private val h = hi.toDouble()
    private val rng = Mulberry32(seed)
    private val path = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun draw(pattern: Int) {
        val bg = if (inv) pal.colors.last() else pal.colors.first()
        c.drawColor(bg)
        when (pattern) {
            0 -> hills()
            1 -> waves()
            2 -> dunes()
            3 -> mountains()
            4 -> arcs()
            5 -> scribble()
        }
    }

    // ---------- helpers ----------

    private fun Path.mv(x: Double, y: Double) = moveTo(x.toFloat(), y.toFloat())
    private fun Path.ln(x: Double, y: Double) = lineTo(x.toFloat(), y.toFloat())

    private fun noise(x: Double): Double {
        val ix = floor(x)
        val fx = x - ix
        val t = fx * fx * (3 - 2 * fx)
        val sa = sin(ix * 127.1 + seed * 0.01) * 43758.5453
        val sb = sin((ix + 1) * 127.1 + seed * 0.01) * 43758.5453
        val a = sa - floor(sa)
        val b = sb - floor(sb)
        return a + (b - a) * t
    }

    private fun fbm(x: Double, octaves: Int = 4): Double {
        var v = 0.0
        var amp = 0.5
        var freq = 1.0
        for (i in 0 until octaves) {
            v += amp * noise(x * freq)
            amp *= 0.5
            freq *= 2.1
        }
        return v
    }

    /** Colour on the palette ramp for t in 0..1 (flipped in light mode). */
    private fun col(t: Double): Int {
        val colors = pal.colors
        val ct = if (inv) 1 - t else t
        val idx = ct * (colors.size - 1)
        val i = floor(idx).toInt()
        val f = idx - i
        if (i >= colors.size - 1) return colors.last()
        if (i < 0) return colors.first()
        val a = colors[i]
        val b = colors[i + 1]
        fun mix(x: Int, y: Int) = (x + (y - x) * f).roundToInt().coerceIn(0, 255)
        return Color.rgb(
            mix(Color.red(a), Color.red(b)),
            mix(Color.green(a), Color.green(b)),
            mix(Color.blue(a), Color.blue(b)),
        )
    }

    // ---------- patterns ----------

    private fun pine(x: Double, bottomY: Double, width: Double, height: Double) {
        path.reset()
        path.mv(x - width * 0.08, bottomY)
        path.ln(x - width * 0.08, bottomY - height * 0.15)
        path.ln(x + width * 0.08, bottomY - height * 0.15)
        path.ln(x + width * 0.08, bottomY)
        path.close()
        c.drawPath(path, fill)

        val segments = 30
        for (i in 0 until segments) {
            val t = i / (segments - 1).toDouble()
            val topY = bottomY - height + height * t * 0.45
            val botY = bottomY - height + height * (t + 0.22)
            val segW = width * (0.25 + t * 0.75)
            val jl = (rng() - 0.5) * (width * 0.08)
            val jr = (rng() - 0.5) * (width * 0.08)
            path.reset()
            path.mv(x, topY)
            path.ln(x - segW / 2 + jl, botY)
            path.ln(x + segW / 2 + jr, botY)
            path.close()
            c.drawPath(path, fill)
        }
    }

    private fun hills() {
        val layers = 5
        val landscape = w > h
        for (i in 0 until layers) {
            val t = (i + 1).toDouble() / layers
            val spacing = if (landscape) 0.40 else 0.33
            val baseY = h * (0.5 + (i.toDouble() / layers) * spacing)
            fill.color = col(t * 0.85 + 0.1)

            path.reset()
            path.mv(0.0, h)
            val rx = ArrayList<Double>()
            val ry = ArrayList<Double>()
            val step = max(4, (w / 120).roundToInt()).toDouble()
            var x = 0.0
            while (x <= w + step) {
                val nx = x / w
                val y = if (i < 2) {
                    val amp = if (landscape) 0.32 else 0.12
                    val freq = if (landscape) 4.5 else 2.5
                    baseY + fbm(nx * freq + i * 4.5, 10) * h * amp - (if (landscape) h * 0.12 else h * 0.05)
                } else {
                    val amp = if (landscape) 0.16 else 0.06
                    val freq = if (landscape) 2.5 else 1.5
                    baseY + fbm(nx * freq + i * 2.1, 8) * h * amp - (if (landscape) h * 0.01 else h * 0.02)
                }
                rx.add(x)
                ry.add(y)
                path.ln(x, y)
                x += step
            }
            path.ln(w, h)
            path.ln(0.0, h)
            path.close()
            c.drawPath(path, fill)

            if (i >= 1) {
                val treeW = if (landscape) h * (0.045 + i * 0.004) else w * (0.12 + i * 0.004)
                val treeH = treeW * (1 + rng() * 0.2)
                val stepX = treeW * 0.35
                var tx = 0.0
                while (tx <= w) {
                    val idx = floor((tx / w) * (ry.size - 1)).toInt().coerceIn(0, ry.size - 1)
                    val ridgeY = ry[idx]
                    val rows = if (landscape) {
                        if (i == 1) 3 else 5 + (layers - i)
                    } else {
                        if (i == 1) 1 else 2 + (layers - i)
                    }
                    for (row in 0 until rows) {
                        val bottomY = ridgeY + row * (treeH * 0.22)
                        if (bottomY > h + 20) continue
                        val scale = if (landscape) 0.65 + rng() * 1.1 else 0.8 + rng() * 0.4
                        val cw = treeW * (if (landscape) min(scale, 1.1) else scale)
                        val ch = treeH * scale
                        val cx = tx + (rng() - 0.5) * (stepX * 0.5)
                        pine(cx, bottomY, cw, ch)
                    }
                    tx += stepX
                }
            }
        }
    }

    private fun waves() {
        val layers = 8 + (rng() * 5).toInt()
        val centerX = w * (0.45 + rng() * 0.1)
        val baseY = h * (0.75 + rng() * 0.08)
        for (i in layers downTo 0) {
            val t = i.toDouble() / layers
            val spread = h * (0.8 + t * 1.8)
            val peakH = h * (0.05 + t * 0.3)
            val skew = rng() * 0.2 - 0.10
            path.reset()
            path.mv(0.0, h)
            path.ln(0.0, baseY + peakH * 0.5)
            val steps = 120
            for (s in 0..steps) {
                val x = s.toDouble() / steps * w
                val dist = (x - centerX) / spread
                val bell = exp(-dist * dist * 0.4)
                val wave = bell * peakH * (1 + skew * dist)
                val micro = fbm((x / h) * 2 + i * 1.4, 3) * h * 0.008
                path.ln(x, baseY - wave + micro)
            }
            path.ln(w, h)
            path.close()
            fill.color = col(0.1 + (1 - t) * 0.8)
            c.drawPath(path, fill)
        }
    }

    private fun dunes() {
        val layers = 7 + (rng() * 5).toInt()
        for (i in 0 until layers) {
            val t = (i + 1).toDouble() / layers
            val baseY = h * (0.55 + t * 0.35)
            val freq = 0.5 + rng() * 0.8
            val phase = rng() * 10
            path.reset()
            var x = 0.0
            var first = true
            while (x <= w) {
                val nx = x / h
                val wave = sin(nx * PI * freq + phase) * h * 0.05
                val n = fbm(nx * 0.8 + i * 2.1, 3) * h * 0.05
                val y = baseY + wave + n
                if (first) {
                    path.mv(x, y)
                    first = false
                } else {
                    path.ln(x, y)
                }
                x += 2.0
            }
            path.ln(w, h)
            path.ln(0.0, h)
            path.close()
            fill.color = col(t * 0.8 + 0.15)
            c.drawPath(path, fill)
        }
    }

    private fun mountains() {
        val layers = 5 + (rng() * 3).toInt()
        for (i in 0 until layers) {
            val t = (i + 1).toDouble() / layers
            val baseY = h * (0.55 + t * 0.35)
            val peakCount = 2 + (rng() * 2).toInt()
            val offset = rng() * 50
            val px = DoubleArray(peakCount)
            val ph = DoubleArray(peakCount)
            val pw = DoubleArray(peakCount)
            for (p in 0 until peakCount) {
                px[p] = w * (0.1 + rng() * 0.8)
                ph[p] = h * (0.1 + rng() * 0.15) * (1 - i * 0.08)
                pw[p] = h * (0.6 + rng() * 0.6)
            }
            path.reset()
            path.mv(-2.0, h + 2)
            var x = -2.0
            while (x <= w + 2) {
                var y = baseY
                for (p in 0 until peakCount) {
                    val dist = abs(x - px[p])
                    if (dist < pw[p]) {
                        val rise = 1 - dist / pw[p]
                        val sharp = 1.3 + rng() * 0.3
                        y = min(y, baseY - rise.pow(sharp) * ph[p])
                    }
                }
                val micro = fbm((x / h) * 1.5 + i * 2.3 + offset, 3) * h * 0.008
                path.ln(x, y + micro)
                x += 2.0
            }
            path.ln(w + 2, h + 2)
            path.close()
            fill.color = col(t * 0.8 + 0.12)
            c.drawPath(path, fill)
        }
    }

    private fun arcs() {
        val originX = w * (0.45 + rng() * 0.1)
        val originY = h * 1.5
        val rings = 14 + (rng() * 6).toInt()
        for (i in rings downTo 0) {
            val t = i.toDouble() / rings
            fill.color = col(0.1 + (1 - t) * 0.8)
            c.drawCircle(originX.toFloat(), originY.toFloat(), (h * t).toFloat(), fill)
        }
    }

    private fun scribble() {
        val widthScale = max(1.0, h / 1200.0)
        for (i in 0 until 15) {
            val t = i / 15.0
            var x = w * rng()
            var y = h * (0.5 + rng() * 0.5)
            path.reset()
            path.mv(x, y)
            for (j in 0 until 10) {
                x += (rng() - 0.5) * w
                y += (rng() - 0.4) * h * 0.2
                val cp1y = h * (0.5 + rng() * 0.5)
                val cp2y = h * (0.5 + rng() * 0.5)
                val cp1x = w * rng()
                val cp2x = w * rng()
                path.cubicTo(cp1x.toFloat(), cp1y.toFloat(), cp2x.toFloat(), cp2y.toFloat(), x.toFloat(), y.toFloat())
            }
            stroke.color = col(t)
            stroke.strokeWidth = (rng() * 3 * widthScale).toFloat()
            c.drawPath(path, stroke)
        }
    }
}
