package com.reyzie.hymns.ui.widgets

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File
import kotlin.math.max
import kotlin.math.min

private const val MAX_BITMAP_DIMENSION = 1600

/** One open PDF file + renderer for the whole viewer session (avoids reopening per page). */
class PdfRenderSession(file: File) : Closeable {
    private val pfd: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer: PdfRenderer = PdfRenderer(pfd)
    val pageCount: Int get() = renderer.pageCount

    fun renderPage(pageIndex: Int): Bitmap {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
            throw IllegalArgumentException("Invalid page index")
        }
        renderer.openPage(pageIndex).use { page ->
            val longestSide = max(page.width, page.height).coerceAtLeast(1)
            val scale = min(MAX_BITMAP_DIMENSION.toFloat() / longestSide, 2f)
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    override fun close() {
        renderer.close()
        pfd.close()
    }
}
