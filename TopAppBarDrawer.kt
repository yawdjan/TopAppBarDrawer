package com.example.app.ui.components.drawer

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DrawerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import com.example.app.data.model.DragAnchorsAt
import com.example.app.ui.components.getScreenHeightInPx
import kotlinx.coroutines.launch
import kotlin.isNaN
import kotlin.math.roundToInt

@Composable
fun getScreenHeightInPx(): Float {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    return configuration.screenHeightDp * density
}

@Composable
fun TopAppBarDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerGesturesEnabled: Boolean = true,
    fadeColor: Color = DrawerDefaults.scrimColor,
    screenHeight: Float = getScreenHeightInPx(),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var anchorsInitialized by remember { mutableStateOf(false) }
    var minValue by remember(density) { mutableStateOf(-screenHeight + 200f ) }
    val maxValue = 0f
    var offsetY by remember { mutableFloatStateOf(0f) }
    //replace with your own anchored draggable state will fix the other l
    //layout stuff later
    val offsetAnimationState = remember {
        AnchoredDraggableState<DragAnchorsAt>(
            initialValue = DragAnchorsAt.Top
        )
    }
    val anchors = remember(density) {
        DraggableAnchors {
            DragAnchorsAt.Top at (-screenHeight + 200f)
            DragAnchorsAt.Bottom at 0f
        }
    }

    SideEffect { offsetAnimationState.updateAnchors(anchors)  }
    //For custom dragging no more using drawer states yk
    val isDrawerOpen = offsetAnimationState.currentValue == DragAnchorsAt.Bottom

    Box(
        modifier = Modifier
            .fillMaxWidth()
//            .pointerInput(Unit) {
//                detectDragGestures {
//                }
//            }
            .offset(
                0.dp , 0.dp
            )
            .anchoredDraggable(
                state = offsetAnimationState,
                orientation = Orientation.Vertical,
                enabled = drawerGesturesEnabled
            )
            .onGloballyPositioned{
                println("TopAppBarDrawer current value ${offsetAnimationState.currentValue} and velocity ${offsetAnimationState.lastVelocity}")
                println("TopAppBarDrawer current offset ${offsetAnimationState.offset} and target value ${offsetAnimationState.targetValue}")
            }
    ) {
        Box{ content() }
        Scrim(
            open = offsetAnimationState.currentValue == DragAnchorsAt.Bottom,
            onClose = {
                scope.launch {
                    offsetAnimationState.animateTo(DragAnchorsAt.Top)
                }
            },
            fraction = { calculateFraction(minValue, maxValue, offsetAnimationState.requireOffset()) },
            color = fadeColor
        )
        val newScreenHeight = getScreenHeightInPx()
        Layout(
            content = drawerContent,
            modifier = Modifier
                .offset {
                    println("offsetY changed to $offsetY")
                    offsetY = offsetAnimationState.requireOffset()
                    IntOffset(0, (0f + offsetAnimationState.requireOffset()).roundToInt())
                }
                .anchoredDraggable(
                    offsetAnimationState,
                    Orientation.Vertical
                )
                .semantics {
//                paneTitle = navigationMenu
                  if (isDrawerOpen)  {
                        dismiss {
                            if (isDrawerOpen) {
                                // Drawer is open, proceed with closing it
                                scope.launch { offsetAnimationState.animateTo(DragAnchorsAt.Top) }
                            }
                            true // Always return true to indicate the action is handled
                        }
                    }
                }
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measurables.fastMap { it.measure(looseConstraints) }
            val width = placeables.fastMaxOfOrNull { it.width } ?: 0
            val height = placeables.fastMaxOfOrNull { it.height } ?: 0

            layout(width, height) {
                val currentClosedAnchor =
                    offsetAnimationState.anchors.positionOf(DragAnchorsAt.Top)
                val calculatedClosedAnchor = (-newScreenHeight + 200f)

                if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
                    if (!anchorsInitialized) {
                        anchorsInitialized = true
                    }
                    minValue = calculatedClosedAnchor
                    offsetAnimationState.updateAnchors(
                        DraggableAnchors {
                            DragAnchorsAt.Top at minValue
                            DragAnchorsAt.Bottom at maxValue
                        }
                    )
                }
                placeables.fastForEach { it.placeRelative(0, 0) }
            }
        }
    }
}

/**
 * A scrim that can be displayed on top of content.
 *
 * @param open Whether the scrim is open.
 * @param onClose Callback to close the scrim.
 * @param fraction The fraction of the scrim that is visible.
 * @param color The color of the scrim.
 */

@Composable
private fun Scrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    Canvas(
        Modifier
            .fillMaxSize()
    ) {
        drawRect(color, alpha = fraction())
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)