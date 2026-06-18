package com.reyzie.hymns.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@Composable
fun PdfSongViewer(
    pdfPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pages by remember(pdfPath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember(pdfPath) { mutableStateOf(true) }
    var error by remember(pdfPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfPath) {
        loading = true
        error = null
        pages = emptyList()
        try {
            pages = withContext(Dispatchers.IO) {
                renderPdfPages(context, pdfPath)
            }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Could not open PDF"
        } finally {
            loading = false
        }
    }

    DisposableEffect(pages) {
        onDispose {
            pages.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading -> ExpressiveCircularProgress(modifier = Modifier.align(Alignment.Center))
            error != null -> Text(
                text = error ?: "Error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
            pages.isEmpty() -> Text(
                text = "No pages in PDF",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(pages, key = { index, _ -> index }) { _, bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

private fun renderPdfPages(context: Context, pdfPath: String): List<Bitmap> {
    val localFile = resolvePdfFile(context, pdfPath)
    val pfd = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        ?: throw IllegalStateException("Could not open PDF file")
    val renderer = PdfRenderer(pfd)
    val bitmaps = mutableListOf<Bitmap>()
    try {
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmaps.add(bitmap)
        }
    } finally {
        renderer.close()
        pfd.close()
    }
    return bitmaps
}

private fun resolvePdfFile(context: Context, pdfPath: String): File {
    return when {
        pdfPath.startsWith("http://", ignoreCase = true) ||
            pdfPath.startsWith("https://", ignoreCase = true) -> downloadToCache(context, pdfPath)
        pdfPath.startsWith("content://") -> copyUriToCache(context, Uri.parse(pdfPath))
        pdfPath.startsWith("file://") -> File(Uri.parse(pdfPath).path ?: pdfPath)
        else -> File(pdfPath)
    }
}

private fun downloadToCache(context: Context, url: String): File {
    val cacheDir = File(context.cacheDir, "pdf_cache").apply { mkdirs() }
    val key = MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
    val outFile = File(cacheDir, "$key.pdf")
    if (outFile.exists() && outFile.length() > 0) return outFile
    val client = OkHttpClient()
    val response = client.newCall(Request.Builder().url(url).build()).execute()
    if (!response.isSuccessful) throw IllegalStateException("Download failed (${response.code})")
    response.body?.byteStream()?.use { input ->
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
    } ?: throw IllegalStateException("Empty PDF response")
    return outFile
}

private fun copyUriToCache(context: Context, uri: Uri): File {
    val cacheDir = File(context.cacheDir, "pdf_cache").apply { mkdirs() }
    val outFile = File(cacheDir, "picked_${System.currentTimeMillis()}.pdf")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
    } ?: throw IllegalStateException("Could not read PDF")
    return outFile
}
