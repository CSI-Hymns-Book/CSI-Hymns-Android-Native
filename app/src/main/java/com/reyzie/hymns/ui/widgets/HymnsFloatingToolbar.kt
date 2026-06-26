@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.reyzie.hymns.ui.widgets

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.ui.navigation.Screen
import com.reyzie.hymns.ui.theme.contentOn
import com.reyzie.hymns.utils.HapticFeedbackManager

/**
 * Bottom tab bar styled like [Essentials](https://github.com/sameerasw/essentials):
 * vibrant primary toolbar with main tabs; Favorites sits in the FAB slot, separate from the bar.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HymnsFloatingToolbar(
    screens: List<Screen>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isChristmasMode: Boolean = false,
    expanded: Boolean = true,
    favoritesScreen: Screen? = null,
    isFavoritesSelected: Boolean = false,
    onFavoritesSelected: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val screenWidth = configuration.screenWidthDp

    val hideSelectedLabel = fontScale > 1.6f || screenWidth < 340

    val scheme = MaterialTheme.colorScheme
    val barIconColor = scheme.primary.contentOn()

    HorizontalFloatingToolbar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
        expanded = expanded,
        floatingActionButton = {
            if (favoritesScreen != null && onFavoritesSelected != null) {
                val favLabel = favoritesScreen.title

                FloatingActionButton(
                    onClick = {
                        if (!isFavoritesSelected) {
                            HapticFeedbackManager.smoothClick(context)
                            onFavoritesSelected()
                        }
                    },
                    containerColor = if (isFavoritesSelected) {
                        scheme.background
                    } else {
                        scheme.primaryContainer
                    },
                    contentColor = if (isFavoritesSelected) {
                        scheme.primary
                    } else {
                        scheme.primaryContainer.contentOn()
                    },
                    shape = MaterialTheme.shapes.large,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isFavoritesSelected) {
                            favoritesScreen.icon
                        } else {
                            favoritesScreen.unselectedIcon
                        },
                        contentDescription = favLabel,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = barIconColor,
            toolbarContainerColor = scheme.primary,
        ),
        content = {
            screens.forEachIndexed { index, screen ->
                val isSelected = selectedIndex == index
                val label = if (isChristmasMode && screen == Screen.Hymns) "Songs" else screen.title

                val itemWidth by animateDpAsState(
                    targetValue = if (expanded || isSelected) 48.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tab_icon_width_$index"
                )

                val labelWidth by animateDpAsState(
                    targetValue = if (isSelected && !hideSelectedLabel) 88.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tab_label_width_$index"
                )

                val spacerWidth by animateDpAsState(
                    targetValue = if (index < screens.lastIndex) 8.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tab_spacer_$index"
                )

                if (itemWidth > 0.dp || isSelected) {
                    IconButton(
                        onClick = {
                            if (!isSelected) {
                                HapticFeedbackManager.smoothClick(context)
                                onTabSelected(index)
                            }
                        },
                        modifier = Modifier
                            .width(itemWidth + labelWidth)
                            .height(48.dp),
                        colors = if (isSelected) {
                            IconButtonDefaults.filledIconButtonColors(
                                contentColor = scheme.primary,
                                containerColor = scheme.background
                            )
                        } else {
                            IconButtonDefaults.iconButtonColors(
                                contentColor = barIconColor,
                                containerColor = scheme.primary
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) screen.icon else screen.unselectedIcon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) scheme.primary else barIconColor
                            )
                            if (isSelected && !hideSelectedLabel) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    color = scheme.primary,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    if (index < screens.lastIndex) {
                        Spacer(modifier = Modifier.width(spacerWidth))
                    }
                }
            }
        }
    )
}
