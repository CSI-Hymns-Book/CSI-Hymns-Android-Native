package com.reyzie.hymns.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String, 
    val title: String, 
    val icon: ImageVector,
    val unselectedIcon: ImageVector = icon
) {
    object Hymns : Screen("hymns", "Hymns", Icons.Filled.MusicNote, Icons.Outlined.MusicNote)
    object Keerthane : Screen("keerthane", "Keerthane", Icons.Filled.Album, Icons.Outlined.Album)
    object Service : Screen("service", "Service", Icons.Filled.EventNote, Icons.Outlined.EventNote)
    object Categories : Screen("categories", "Categories", Icons.Filled.Category, Icons.Outlined.Category)
    object Favorites : Screen("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    object Auth : Screen("auth", "Sign In", Icons.Filled.Person, Icons.Outlined.Person)
}

val mainNavScreens = listOf(
    Screen.Hymns,
    Screen.Keerthane,
    Screen.Service,
    Screen.Categories
)

val christmasNavScreens = listOf(
    Screen.Hymns,
    Screen.Service,
    Screen.Categories
)

val mainScreens = mainNavScreens + Screen.Favorites

val christmasMainScreens = christmasNavScreens + Screen.Favorites
