package com.example.wllpr

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** In-memory cache of rendered bitmaps (thumbnails and previews). */
object BitmapCache {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bmp: Bitmap) {
        cache.put(key, bmp)
    }
}

object Device {
    /** Portrait pixel size of this phone's screen, capped at 1440 px wide. */
    fun size(): Pair<Int, Int> {
        val dm = Resources.getSystem().displayMetrics
        var w = min(dm.widthPixels, dm.heightPixels)
        var h = max(dm.widthPixels, dm.heightPixels)
        if (w > 1440) {
            val scale = 1440f / w
            w = 1440
            h = (h * scale).roundToInt()
        }
        return w to h
    }

    /** Height / width of the screen. */
    fun aspect(): Float {
        val (w, h) = size()
        return h.toFloat() / w
    }
}

object Actions {
    const val TARGET_HOME = 0
    const val TARGET_LOCK = 1
    const val TARGET_BOTH = 2

    /** Renders at full screen resolution and sets it as the wallpaper. */
    suspend fun setWallpaper(context: Context, spec: Spec, target: Int): Boolean {
        val app = context.applicationContext
        val wm = WallpaperManager.getInstance(app)
        if (!wm.isWallpaperSupported || !wm.isSetWallpaperAllowed) return false
        val (w, h) = Device.size()
        val bmp = withContext(Dispatchers.Default) { Engine.render(w, h, spec) }
        return withContext(Dispatchers.IO) {
            try {
                val flags = when (target) {
                    TARGET_HOME -> WallpaperManager.FLAG_SYSTEM
                    TARGET_LOCK -> WallpaperManager.FLAG_LOCK
                    else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wm.setBitmap(bmp, null, true, flags)
                true
            } catch (e: Exception) {
                false
            } finally {
                bmp.recycle()
            }
        }
    }

    /** Renders at full screen resolution and saves a PNG to Pictures/WLLPR. */
    suspend fun saveToGallery(context: Context, spec: Spec, name: String): Boolean {
        val app = context.applicationContext
        val (w, h) = Device.size()
        val bmp = withContext(Dispatchers.Default) { Engine.render(w, h, spec) }
        return withContext(Dispatchers.IO) {
            val resolver = app.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WLLPR")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                bmp.recycle()
                false
            } else {
                try {
                    val out = resolver.openOutputStream(uri)
                    if (out == null) {
                        resolver.delete(uri, null, null)
                        false
                    } else {
                        out.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                        resolver.update(uri, done, null, null)
                        true
                    }
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    false
                } finally {
                    bmp.recycle()
                }
            }
        }
    }
}
