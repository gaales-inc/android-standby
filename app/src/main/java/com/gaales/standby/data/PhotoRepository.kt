package com.gaales.standby.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

data class Album(val id: String, val name: String, val photos: List<Uri>)

/** Albums and photos for the Photos view and the Photos widget. */
object PhotoRepository {
    const val FEATURED_ID = "featured"

    private val cache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    @Volatile private var albumsCache: Pair<Long, List<Album>>? = null

    fun hasPermission(context: Context): Boolean {
        fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= 34 -> granted(Manifest.permission.READ_MEDIA_IMAGES) || granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
            Build.VERSION.SDK_INT >= 33 -> granted(Manifest.permission.READ_MEDIA_IMAGES)
            else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** "Featured" (a shuffled pick of recent photos, minus screenshots) followed by the largest folders. */
    suspend fun albums(context: Context): List<Album> = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext emptyList()
        albumsCache?.let { (at, list) -> if (System.currentTimeMillis() - at < 10 * 60_000L) return@withContext list }
        val all = mutableListOf<Pair<Uri, String>>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { c ->
                while (c.moveToNext() && all.size < 3000) {
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
                    all += uri to (c.getString(1) ?: "Photos")
                }
            }
        }
        if (all.isEmpty()) return@withContext emptyList()
        val featured = all.filterNot { it.second.contains("screenshot", ignoreCase = true) }
            .ifEmpty { all }
            .take(800)
            .shuffled()
            .take(150)
            .map { it.first }
        val buckets = all.groupBy { it.second }
            .filter { it.value.size >= 3 }
            .entries
            .sortedByDescending { it.value.size }
            .take(8)
            .map { (name, items) -> Album("bucket:$name", name, items.take(300).map { it.first }.shuffled()) }
        val result = listOf(Album(FEATURED_ID, "Featured", featured)) + buckets
        albumsCache = System.currentTimeMillis() to result
        result
    }

    /** Decodes a photo no larger than [maxDim] on its long edge, honoring EXIF rotation on API 28+. */
    suspend fun load(context: Context, uri: Uri, maxDim: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$maxDim"
        cache.get(key)?.let { return@withContext it }
        val bitmap = runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val longest = max(info.size.width, info.size.height)
                    var sample = 1
                    while (longest / (sample * 2) >= maxDim) sample *= 2
                    decoder.setTargetSampleSize(sample)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val longest = max(bounds.outWidth, bounds.outHeight)
                var sample = 1
                while (longest / (sample * 2) >= maxDim) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            }
        }.getOrNull()
        if (bitmap != null) cache.put(key, bitmap)
        bitmap
    }
}
