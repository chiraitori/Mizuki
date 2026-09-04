package dev.chiraitori.mizuki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.mizuki.ui.screens.MainTab

@Composable
fun PixelPlayerStyleNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    activeDownloadCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                val label = stringResource(tab.labelRes)

                AnimatedNavBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        if (tab == MainTab.HOME && activeDownloadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(activeDownloadCount.toString())
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = label)
                            }
                        } else {
                            Icon(tab.icon, contentDescription = label)
                        }
                    },
                    label = label
                )
            }
        }
    }
}

@Composable
private fun RowScope.AnimatedNavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "textColor"
    )

    // PixelPlayer Bouncy Spring Scale on Select
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(60.dp, 32.dp)
        ) {
            // Pill shape indicator with bouncy scaleIn and smooth fade
            this@Column.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(120)) +
                        scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialScale = 0.75f
                        ),
                exit = fadeOut(animationSpec = tween(100)) +
                        scaleOut(animationSpec = tween(100, easing = EaseInQuart))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            ) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    icon()
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        ProvideTextStyle(
            value = MaterialTheme.typography.labelMedium.copy(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        ) {
            Text(text = label)
        }
    }
}
