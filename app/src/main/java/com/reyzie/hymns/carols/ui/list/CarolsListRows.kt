package com.reyzie.hymns.carols.ui.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.churchColorSlot
import com.reyzie.hymns.carols.domain.churchInitials
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.lists.CHRISTMAS_FIELD_CORNER_DP
import com.reyzie.hymns.ui.christmas.lists.ChristmasFieldShape
import com.reyzie.hymns.ui.christmas.lists.ChristmasRowShape
import com.reyzie.hymns.ui.christmas.lists.ChristmasSongRow
import com.reyzie.hymns.ui.christmas.lists.christmasGlass
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette

private const val SHIMMER_MILLIS = 1_200
private const val SHIMMER_ROWS = 6

/**
 * Search box for churches and songs, frosted glass with a snow cap. Typing English finds Kannada
 * songs and the other way round. With [onClose] it has a close button and can take focus on open.
 */
@Composable
fun CarolsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    autoFocus: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    val palette = rememberChristmasPalette()
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) focus.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = palette.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = palette.gold) },
        trailingIcon = {
            when {
                onClose != null -> IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close search", tint = palette.inkMuted)
                }
                query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = palette.inkMuted)
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .christmasGlass(ChristmasFieldShape, CHRISTMAS_FIELD_CORNER_DP, high = true)
            .focusRequester(focus),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = palette.ink,
            unfocusedTextColor = palette.ink,
            cursorColor = palette.gold,
        ),
    )
}

/** Shown when no church matches (or none have synced yet). */
@Composable
fun EmptyChurchesState(syncHint: String?, isAuthenticated: Boolean, onRefresh: () -> Unit) {
    val palette = rememberChristmasPalette()
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⛪", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("No churches yet", color = palette.ink, fontFamily = ChristmasDisplayFont, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            syncHint ?: if (isAuthenticated) "Tap + to add your church, or pull down to sync." else "Sign in to contribute, or pull down to browse.",
            color = palette.inkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.gold)
            Spacer(Modifier.width(8.dp))
            Text("Sync", color = palette.ink)
        }
    }
}

/** A church: coloured initials avatar, name, and how many songs and PDFs it has. */
@Composable
fun ChurchListCard(
    church: CarolChurch,
    songCount: Int,
    pdfCount: Int,
    canDelete: Boolean,
    isDefault: Boolean,
    onTap: () -> Unit,
    onToggleDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = rememberChristmasPalette()
    var confirmDelete by remember { mutableStateOf(false) }
    ChristmasSongRow(
        label = if (isDefault) "Default · ${churchCountLabel(songCount, pdfCount)}" else churchCountLabel(songCount, pdfCount),
        title = church.name,
        subtitle = church.description?.takeIf { it.isNotBlank() },
        badgeBackground = Color.Transparent,
        onClick = onTap,
        trailing = { p ->
            DefaultChurchButton(church.name, isDefault, p, onToggleDefault)
            if (canDelete) RowOverflowMenu(church.name, p, onEdit = null, onDelete = { confirmDelete = true }) else Chevron(p)
        },
    ) { ChurchAvatar(church.name, palette) }
    if (confirmDelete) {
        // A church takes its songs and PDFs with it, so it asks first like they do.
        ConfirmDelete(
            "Delete church?",
            "Remove ${church.name} and its ${churchCountLabel(songCount, pdfCount)}? This cannot be undone.",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Round avatar with the church's initials on a colour picked from its name. */
@Composable
fun ChurchAvatar(name: String, palette: ChristmasPalette) {
    val colors = listOf(palette.pine, palette.ice, palette.ember, palette.holly, palette.gold)
    val color = colors[churchColorSlot(name, colors.size)]
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.45f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(churchInitials(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun churchCountLabel(songCount: Int, pdfCount: Int): String = buildString {
    append("$songCount song${if (songCount == 1) "" else "s"}")
    if (pdfCount > 0) append(" · $pdfCount PDF${if (pdfCount == 1) "" else "s"}")
}

/** Sort menu for a church's songs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortChipRow(viewModel: CommunityCarolsViewModel) {
    val palette = rememberChristmasPalette()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            AssistChip(
                onClick = { expanded = true },
                label = { Text(if (sortOrder == CarolSortOrder.NUMBER) "By number" else "Newest", color = palette.ink) },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = palette.gold) },
                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = palette.edge),
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("By song number") }, onClick = {
                    viewModel.setSortOrder(CarolSortOrder.NUMBER)
                    expanded = false
                })
                DropdownMenuItem(text = { Text("Newest first") }, onClick = {
                    viewModel.setSortOrder(CarolSortOrder.NEWEST)
                    expanded = false
                })
            }
        }
    }
}

/**
 * A carol song row with a gold star when it is a favourite. People who may manage the song get a
 * three-dot menu with Edit and Delete; everyone else sees a chevron.
 */
@Composable
fun SongRow(
    song: CarolSong,
    isFavourite: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = rememberChristmasPalette()
    var confirmDelete by remember { mutableStateOf(false) }
    ChristmasSongRow(
        label = song.songNumber?.takeIf { it.isNotBlank() }?.let { "No. $it" } ?: "Carol",
        title = song.title,
        subtitle = song.scale.takeIf { it.isNotBlank() }?.let { "Scale: $it" },
        badgeBackground = palette.pineBackground,
        emphasizeLabel = !song.songNumber.isNullOrBlank(),
        onClick = onOpen,
        trailing = { p ->
            if (isFavourite) Icon(Icons.Default.Star, contentDescription = "Favourite", tint = p.gold, modifier = Modifier.size(18.dp))
            if (canEdit || canDelete) {
                RowOverflowMenu(
                    song.title,
                    p,
                    onEdit = if (canEdit) onEdit else null,
                    onDelete = if (canDelete) ({ confirmDelete = true }) else null,
                )
            } else {
                Chevron(p)
            }
        },
    ) { CarolSongIcon(palette) }
    if (confirmDelete) {
        ConfirmDelete("Delete song?", "Remove ${song.title}?", onConfirm = onDelete, onDismiss = { confirmDelete = false })
    }
}

/** A sheet-music PDF row. */
@Composable
fun PdfRow(pdf: CarolPdf, canDelete: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val palette = rememberChristmasPalette()
    var confirmDelete by remember { mutableStateOf(false) }
    ChristmasSongRow(
        label = pdf.songNumber?.let { "No. $it" } ?: "Sheet music",
        title = pdf.title,
        subtitle = "Tap to open PDF",
        badgeBackground = palette.emberBackground,
        onClick = onOpen,
        trailing = { p ->
            if (canDelete) RowOverflowMenu(pdf.title, p, onEdit = null, onDelete = { confirmDelete = true }) else Chevron(p)
        },
    ) { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = palette.ember) }
    if (confirmDelete) {
        ConfirmDelete("Delete PDF?", "Remove ${pdf.title}?", onConfirm = onDelete, onDismiss = { confirmDelete = false })
    }
}

/**
 * "Your carols" strip above the churches: favourites first, then recently opened songs,
 * as small tappable chips. Hidden when there is nothing to show.
 */
@Composable
fun YourCarolsStrip(
    favourites: List<CarolSong>,
    recents: List<CarolSong>,
    onOpen: (CarolSong) -> Unit,
) {
    if (favourites.isEmpty() && recents.isEmpty()) return
    val palette = rememberChristmasPalette()
    Column(Modifier.padding(bottom = 6.dp)) {
        if (favourites.isNotEmpty()) StripRow("★  Favourites", favourites, palette, onOpen)
        if (recents.isNotEmpty()) StripRow("Recently opened", recents, palette, onOpen)
    }
}

@Composable
private fun StripRow(heading: String, songs: List<CarolSong>, palette: ChristmasPalette, onOpen: (CarolSong) -> Unit) {
    Text(
        heading.uppercase(),
        color = palette.gold,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(songs, key = { "$heading-${it.id}" }) { song ->
            Text(
                song.title,
                color = palette.ink,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(150.dp)
                    .christmasGlass(RoundedCornerShape(14.dp), 14f, capped = false)
                    .clickable { onOpen(song) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/** Placeholder rows that shimmer gently while carols load for the first time. */
@Composable
fun CarolsShimmer() {
    val palette = rememberChristmasPalette()
    val sweep by rememberInfiniteTransition(label = "carolShimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "carolShimmerSweep",
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(SHIMMER_ROWS) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .height(72.dp)
                    .clip(ChristmasRowShape)
                    .background(palette.glass)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            (sweep - 0.2f).coerceIn(0f, 1f) to Color.Transparent,
                            sweep.coerceIn(0f, 1f) to palette.glassHigh,
                            (sweep + 0.2f).coerceIn(0f, 1f) to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

/** Pin that makes a church the one partner apps open straight to. */
@Composable
private fun DefaultChurchButton(churchName: String, isDefault: Boolean, palette: ChristmasPalette, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (isDefault) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (isDefault) "Default church: $churchName. Tap to clear" else "Set $churchName as the default church",
            tint = if (isDefault) palette.gold else palette.inkMuted,
        )
    }
}

/**
 * Three-dot menu at the end of a row. Edit and Delete live here, out of sight, so nothing on the
 * list invites an accidental delete. A null action leaves its item out.
 */
@Composable
private fun RowOverflowMenu(name: String, palette: ChristmasPalette, onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options for $name", tint = palette.inkMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        open = false
                        onEdit()
                    },
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                    onClick = {
                        open = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun Chevron(palette: ChristmasPalette) {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = palette.gold.copy(alpha = 0.8f),
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun ConfirmDelete(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
