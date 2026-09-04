package dev.chiraitori.mizuki.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Applies a bouncy spring scale reduction when the component is pressed down.
 * Used for cards, buttons, items to feel fluid and tactile (PixelPlayer / iOS fluid physics).
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounceClickScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Applies the bouncy spring scale effect using an existing [MutableInteractionSource].
 */
fun Modifier.bounceOnPress(
    interactionSource: MutableInteractionSource,
    scaleDown: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounceOnPressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Universal touch-based bouncy spring effect that works with any composable
 * (Buttons, Cards, Chips, IconButtons) without interfering with click handlers or ripples.
 */
fun Modifier.bounceOnTouch(scaleDown: Float = 0.96f): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounceTouchScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isPressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    isPressed = false
                }
            }
        }
}
