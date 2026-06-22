package com.reyzie.hymns.ui.widgets

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val httpClient by lazy { OkHttpClient() }

private sealed interface PdfLoadState {
    data object Loading : PdfLoadState
    data class Error(val message: String, val sourceUrl: String?) : PdfLoadState
    data class Ready(val file: File, val pageCount: Int, val sourceUrl: String?) : PdfLoadState
}

@Composable
fun PdfSongViewer(
    pdfPath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageCache = remember(pdfPath) { PdfPageCache() }
    var state by remember(pdfPath) { mutableStateOf<PdfLoadState>(PdfLoadState.Loading) }
    var renderTick by remember(pdfPath) { mutableIntStateOf(0) }

    DisposableEffect(pdfPath) {
        onDispose { pageCache.clear() }
    }

    LaunchedEffect(pdfPath) {
        state = PdfLoadState.Loading
        state = try {
            val file = withContext(Dispatchers.IO) {
                resolvePdfFile(context, pdfPath)
            }
            val pageCount = withContext(Dispatchers.IO) {
                readPageCount(file)
            }
            if (pageCount <= 0) {
                PdfLoadState.Error("No pages in PDF", pdfPath.takeIf { it.startsWith("http") })
            } else {
                PdfLoadState.Ready(file, pageCount, pdfPath.takeIf { it.startsWith("http", ignoreCase = true) })
            }
        } catch (e: Exception) {
            PdfLoadState.Error(e.localizedMessage ?: "Could not open PDF", pdfPath.takeIf { it.startsWith("http") })
        }
    }

    val ready = state as? PdfLoadState.Ready
    val listState = rememberLazyListState()

    LaunchedEffect(ready?.file, ready?.pageCount) {
        val session = ready ?: return@LaunchedEffect
        coroutineScope {
            val renderMutex = Mutex()
            val renderSession = withContext(Dispatchers.IO) { PdfRenderSession(session.file) }
            try {
                suspend fun renderIndex(pageIndex: Int) {
                    if (!isActive || pageIndex < 0 || pageIndex >= session.pageCount) return
                    if (pageCache.has(pageIndex)) return
                    renderMutex.withLock {
                        if (pageCache.has(pageIndex)) return
                        try {
                            val bitmap = renderSession.renderPage(pageIndex)
                            pageCache.put(pageIndex, bitmap)
                            renderTick++
                        } catch (_: Exception) {
                            // Skip failed pages.
                        }
                    }
                }

                val visibleJob = launch {
                    snapshotFlow {
                        listState.layoutInfo.visibleItemsInfo.map { it.index }.distinct()
                    }.collect { visible ->
                        for (pageIndex in visible) {
                            renderIndex(pageIndex)
                            renderIndex(pageIndex - 1)
                            renderIndex(pageIndex + 1)
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    for (pageIndex in 0 until session.pageCount) {
                        if (!isActive) break
                        renderIndex(pageIndex)
                    }
                }
                visibleJob.cancel()
            } finally {
                withContext(Dispatchers.IO) { renderSession.close() }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            PdfLoadState.Loading -> ExpressiveCircularProgress(modifier = Modifier.align(Alignment.Center))
            is PdfLoadState.Error -> PdfErrorState(
                message = current.message,
                modifier = Modifier.align(Alignment.Center),
            )
            is PdfLoadState.Ready -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(current.pageCount, key = { it }) { pageIndex ->
                        PdfPageSlot(
                            pageIndex = pageIndex,
                            pageCache = pageCache,
                            renderTick = renderTick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageSlot(
    pageIndex: Int,
    pageCache: PdfPageCache,
    renderTick: Int,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val tick = renderTick
    val bitmap = pageCache.get(pageIndex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF page ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PdfErrorState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

private fun readPageCount(file: File): Int {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            return renderer.pageCount
        }
    }
}

private fun resolvePdfFile(context: Context, pdfPath: String): File {
    val file = when {
        pdfPath.startsWith("http://", ignoreCase = true) ||
            pdfPath.startsWith("https://", ignoreCase = true) -> downloadToCache(context, pdfPath)
        pdfPath.startsWith("content://") -> copyUriToCache(context, Uri.parse(pdfPath))
        pdfPath.startsWith("file://") -> File(Uri.parse(pdfPath).path ?: pdfPath)
        else -> File(pdfPath)
    }
    if (!file.exists() || file.length() == 0L) {
        throw IllegalStateException("PDF file is missing or empty")
    }
    if (!isPdfFile(file)) {
        throw IllegalStateException("Downloaded file is not a valid PDF")
    }
    return file
}

private fun isPdfFile(file: File): Boolean {
    return file.inputStream().use { input ->
        val header = ByteArray(5)
        input.read(header) == 5 && header.decodeToString().startsWith("%PDF-")
    }
}

private fun downloadToCache(context: Context, url: String): File {
    val cacheDir = File(context.cacheDir, "pdf_cache").apply { mkdirs() }
    val key = MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
    val outFile = File(cacheDir, "$key.pdf")
    if (outFile.exists() && outFile.length() > 0 && isPdfFile(outFile)) return outFile

    val response = httpClient.newCall(
        Request.Builder()
            .url(url)
            .header("Accept", "application/pdf,*/*")
            .build(),
    ).execute()
    if (!response.isSuccessful) {
        throw IllegalStateException("Download failed (${response.code})")
    }
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
