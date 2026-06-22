package com.reyzie.hymns.ui.widgets

import android.graphics.Bitmap
import android.util.LruCache

/** In-memory page bitmap cache for one open PDF session. Survives LazyColumn scroll. */
class PdfPageCache(maxPages: Int = 24) {
    private val cache = object : LruCache<Int, Bitmap>(maxPages) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) oldValue.recycle()
        }
    }

    fun get(pageIndex: Int): Bitmap? = cache.get(pageIndex)

    fun put(pageIndex: Int, bitmap: Bitmap) {
        cache.put(pageIndex, bitmap)
    }

    fun has(pageIndex: Int): Boolean = cache.get(pageIndex) != null

    fun clear() {
        cache.evictAll()
    }
}
