package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.data.CustomCategoriesRepository
import com.reyzie.hymns.data.CustomCategory
import com.reyzie.hymns.ui.widgets.ExpressiveScreenTopBar
import com.reyzie.hymns.utils.HapticFeedbackManager
import com.reyzie.hymns.ui.viewmodels.AuthViewModel
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignInClick: () -> Unit,
    onCategoryClick: (Int, String) -> Unit,
    onRecentSongsClick: () -> Unit,
    onCommonCategoryClick: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    val isAuthenticated = sessionStatus is SessionStatus.Authenticated
    val context = LocalContext.current
    val repository = remember { CustomCategoriesRepository(context) }
    val scope = rememberCoroutineScope()
    
    var loading by remember { mutableStateOf(false) }
    var guestRemaining by remember { mutableStateOf(5) }
    var categories by remember { mutableStateOf<List<CustomCategory>>(emptyList()) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    fun loadCategories() {
        scope.launch {
            val list = repository.getCategories()
            categories = list
            guestRemaining = 5 - list.size
        }
    }
    
    LaunchedEffect(Unit) {
        loadCategories()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExpressiveScreenTopBar(
                title = "Categories",
                onMenuClick = onMenuClick
            )
        }
    ) { padding ->
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Custom Category") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        if (newCategoryName.isNotBlank()) {
                            scope.launch {
                                repository.addCategory(newCategoryName)
                                loadCategories()
                                newCategoryName = ""
                                showCreateDialog = false
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val commonCategories = listOf(
                "Birthday", "Marriage", "House Warming", "Funeral", 
                "Mangala", "Children's Prayer", "Lord's Supper", 
                "Travelling", "Sickness"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "ESSENTIALS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                
                item {
                    CategoryCard(
                        title = "Recent Songs",
                        isGradient = true,
                        onClick = onRecentSongsClick
                    )
                }
                
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "OCCASIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                
                commonCategories.forEach { category ->
                    item {
                        CategoryCard(
                            title = category,
                            onClick = { onCommonCategoryClick(category) }
                        )
                    }
                    if (category == "Sickness") {
                        item {
                            AddCategoryCard(
                                enabled = isAuthenticated || guestRemaining > 0,
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    showCreateDialog = true
                                }
                            )
                        }
                    }
                }
                
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "CUSTOM COLLECTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (!isAuthenticated) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Guest slots left: $guestRemaining/5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    onSignInClick()
                                }) {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign in for more", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                
                items(categories) { cat ->
                    CategoryCard(
                        title = cat.name,
                        onClick = { onCategoryClick(cat.id, cat.name) }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun AddCategoryCard(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick = {
            if (enabled) {
                HapticFeedbackManager.smoothClick(context)
                onClick()
            }
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.5f else 0.2f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "New Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CategoryCard(title: String, isGradient: Boolean = false, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = {
            HapticFeedbackManager.smoothClick(context)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradient) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGradient) 4.dp else 0.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center,
                color = if (isGradient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
