package com.kaii.bphotos.compose.single_photo.editing_view.video_editor

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.kaii.bphotos.R

import com.kaii.bphotos.compose.dialogs.TextEntryDialog
import com.kaii.bphotos.compose.single_photo.EditingViewBottomAppBarItem
import com.kaii.bphotos.helpers.AnimationConstants
import com.kaii.bphotos.helpers.VideoPlayerConstants
import com.kaii.bphotos.helpers.editing.BasicVideoData
import com.kaii.bphotos.helpers.editing.MediaAdjustments
import com.kaii.bphotos.helpers.editing.VideoEditingState
import com.kaii.bphotos.helpers.editing.VideoModification
import kotlin.math.roundToInt

@Composable
fun TrimContent(
    currentPosition: MutableFloatState,
    basicData: BasicVideoData,
    videoEditingState: VideoEditingState,
    thumbnails: List<Bitmap>,
    onSeek: (Float) -> Unit,
) {
    val actualDuration by rememberUpdatedState(videoEditingState.endTrimPosition - videoEditingState.startTrimPosition)

    val rawDuration by rememberUpdatedState(basicData.duration)
    // Guard: duration is 0/unset until ExoPlayer is ready; division by it
    // produced NaN trim-handle offsets (-> crash/jank on open).
    val duration = if (rawDuration.isFinite() && rawDuration > 0f) rawDuration else 1f
    val safeActualDuration = if (actualDuration.isFinite() && actualDuration > 0f) actualDuration else 1f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(1f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val localDensity = LocalDensity.current
        var isDraggingManually by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            // shows video thumbnails
            Row(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                thumbnails.forEach {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "thumbnail image for video",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight(1f)
                            .width(this@BoxWithConstraints.maxWidth / VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - (22.dp / VideoPlayerConstants.TRIM_THUMBNAIL_COUNT.toFloat()))
                    )
                }
            }

            BoxWithConstraints boxybox@{
                val leftHandlePosition by remember(videoEditingState.startTrimPosition) {
                    derivedStateOf {
                        this@boxybox.maxWidth * videoEditingState.startTrimPosition / duration
                    }
                }
                val rightHandlePosition by remember(videoEditingState.endTrimPosition) {
                    derivedStateOf {
                        this@boxybox.maxWidth * videoEditingState.endTrimPosition / duration
                    }
                }
                val seekbarWidth by remember {
                    derivedStateOf {
                        rightHandlePosition - leftHandlePosition - 14.dp
                    }
                }

                val seekHandlePosition by remember {
                    derivedStateOf {
                        leftHandlePosition + seekbarWidth * (currentPosition.floatValue - videoEditingState.startTrimPosition) / safeActualDuration
                    }
                }

                val videoPositionDraggableState = rememberDraggableState { change ->
                    with(localDensity) {
                        val denom = seekbarWidth.toPx()
                        if (!denom.isFinite() || denom == 0f) return@with
                        val new = currentPosition.floatValue + (change * safeActualDuration / denom)
                        currentPosition.floatValue = new.coerceIn(videoEditingState.startTrimPosition, videoEditingState.endTrimPosition)
                        onSeek(currentPosition.floatValue)
                    }
                }

                val animatedSeekOffset by animateDpAsState(
                    targetValue = if (seekHandlePosition.isUnspecified) 0.dp else seekHandlePosition,
                    animationSpec = tween(
                        durationMillis = if (!isDraggingManually) AnimationConstants.DURATION else 0,
                        easing = LinearEasing
                    )
                )
                // seek handle
                Box(
                    modifier = Modifier
                        .offset(x = animatedSeekOffset + 4.dp, y = 4.dp)
                        .width(6.dp)
                        .height(this@boxybox.maxHeight - 8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .draggable(
                            state = videoPositionDraggableState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                isDraggingManually = true
                            },
                            onDragStopped = {
                                isDraggingManually = false
                            }
                        )
                )

                val leftDraggableState = rememberDraggableState { change ->
                    with(localDensity) {
                        val new = videoEditingState.startTrimPosition + (change * duration / this@boxybox.maxWidth.toPx())
                        videoEditingState.setStartTrimPosition(new)

                        currentPosition.floatValue = videoEditingState.startTrimPosition
                    }
                }
                val animatedLeftHandlePos by animateDpAsState(
                    targetValue = if (leftHandlePosition.isUnspecified) 0.dp else leftHandlePosition,
                    animationSpec = AnimationConstants.expressiveTween(
                        durationMillis = if (isDraggingManually) 0 else AnimationConstants.DURATION
                    )
                )
                LeftHandle(
                    handlePosition = animatedLeftHandlePos,
                    height = this@BoxWithConstraints.maxHeight,
                    draggableState = leftDraggableState,
                    onDragStarted = {
                        isDraggingManually = true
                    },
                    onDragStopped = {
                        isDraggingManually = false
                        currentPosition.floatValue = videoEditingState.startTrimPosition
                        onSeek(currentPosition.floatValue)
                    }
                )

                val rightDraggableState = rememberDraggableState { change ->
                    with(localDensity) {
                        val new = videoEditingState.endTrimPosition + (change * duration / this@boxybox.maxWidth.toPx())
                        videoEditingState.setEndTrimPosition(new)

                        currentPosition.floatValue = videoEditingState.endTrimPosition
                    }
                }
                val animatedRightHandlePos by animateDpAsState(
                    targetValue = if (leftHandlePosition.isUnspecified) 32.dp else rightHandlePosition,
                    animationSpec = AnimationConstants.expressiveTween(
                        durationMillis = if (isDraggingManually) 0 else AnimationConstants.DURATION
                    )
                )
                RightHandle(
                    handlePosition = animatedRightHandlePos,
                    draggableState = rightDraggableState,
                    height = this@BoxWithConstraints.maxHeight,
                    onDragStarted = {
                        isDraggingManually = true
                    },
                    onDragStopped = {
                        isDraggingManually = false
                        currentPosition.floatValue = videoEditingState.endTrimPosition
                        onSeek(currentPosition.floatValue)
                    }
                )

                // connector between two handles and gives "inverted rounding" on inside
                Box(
                    modifier = Modifier
                        .offset(x = animatedLeftHandlePos)
                        .width(animatedRightHandlePos - animatedLeftHandlePos)
                        .requiredHeight(this@BoxWithConstraints.maxHeight)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()

                            drawRoundRect(
                                color = Color.White,
                                topLeft = with(localDensity) {
                                    Offset(
                                        x = 0f,
                                        y = 6.dp.toPx()
                                    )
                                },
                                size = with(localDensity) {
                                    Size(
                                        width = (animatedRightHandlePos - animatedLeftHandlePos).toPx(),
                                        height = (this@BoxWithConstraints.maxHeight - 12.dp).toPx()
                                    )
                                },
                                cornerRadius = with(localDensity) {
                                    CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                },
                                blendMode = BlendMode.DstOut,
                            )
                        }
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun LeftHandle(
    handlePosition: Dp,
    height: Dp,
    draggableState: DraggableState,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit
) {
    Box(
        modifier = modifier
            .offset(x = handlePosition - 22.dp)
            .requiredHeight(height)
            .width(22.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 0.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 0.dp
                )
            )
            .background(MaterialTheme.colorScheme.primary)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.6f)
                .width(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
private fun RightHandle(
    handlePosition: Dp,
    height: Dp,
    draggableState: DraggableState,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit
) {
    Box(
        modifier = modifier
            .offset(x = handlePosition)
            .requiredHeight(height)
            .width(24.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 20.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 20.dp
                )
            )
            .background(MaterialTheme.colorScheme.primary)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.6f)
                .width(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
fun VideoEditorProcessingContent(
    basicData: BasicVideoData,
    videoEditingState: VideoEditingState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        var showVolumeDialog by remember { mutableStateOf(false) }
        if (showVolumeDialog) {
            SliderDialog(
                steps = 200,
                range = 0f..200f,
                startsAt = videoEditingState.volume * 100f,
                title = {
                    "Volume: ${it.toInt()}%"
                },
                onSetValue = {
                    videoEditingState.setVolume(it / 100f)
                },
                onDismiss = {
                    showVolumeDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = "Volume",
            iconResId = R.drawable.volume_max,
            onClick = {
                showVolumeDialog = true
            }
        )

        var showSpeedDialog by remember { mutableStateOf(false) }
        fun getSpeed(index: Int) =
            when (index) {
                0 -> 0.5f
                1 -> 1f
                2 -> 1.5f
                3 -> 2f
                4 -> 4f
                5 -> 8f

                else -> 1f
            }

        fun inverseGetSpeed(speed: Float) =
            when (speed) {
                0.5f -> 0f
                1f -> 1f
                1.5f -> 2f
                2f -> 3f
                4f -> 4f
                8f -> 5f

                else -> 1f
            }

        if (showSpeedDialog) {
            SliderDialog(
                steps = 4,
                range = 0f..5f,
                startsAt = inverseGetSpeed(videoEditingState.speed),
                title = {
                    "${getSpeed(it.toInt())}X"
                },
                onSetValue = {
                    videoEditingState.setSpeed(getSpeed(it.toInt()))
                },
                onDismiss = {
                    showSpeedDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = "Speed",
            iconResId = R.drawable.shutter_speed,
            onClick = {
                showSpeedDialog = true
            }
        )

        var showFrameDropDialog by remember { mutableStateOf(false) }
        // Guard: frameRate is 0 until metadata loads; negative steps / inverted
        // range crash the Slider immediately when the dialog opens.
        val maxFps = basicData.frameRate.roundToInt().coerceAtLeast(2)
        if (showFrameDropDialog) {
            SliderDialog(
                steps = (maxFps - 2).coerceAtLeast(0),
                range = 1f..maxFps.toFloat(),
                startsAt = (if (videoEditingState.frameRate == 0f) basicData.frameRate else videoEditingState.frameRate).coerceIn(1f, maxFps.toFloat()),
                title = {
                    "${it.roundToInt()} fps"
                },
                onSetValue = {
                    videoEditingState.setFrameRate(it.roundToInt().toFloat())
                },
                onDismiss = {
                    showFrameDropDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = "FPS",
            iconResId = R.drawable.timer,
            onClick = {
                showFrameDropDialog = true
            }
        )

        var showBitrateDialog by remember { mutableStateOf(false) }
        if (showBitrateDialog) {
            TextEntryDialog(
                title = "Bitrate",
                placeholder = "${videoEditingState.bitrate} bps",
                onConfirm = {
                    val success = it.toIntOrNull() != null && it.toInt() > 0
                    if (success) videoEditingState.setBitrate(it.toInt())

                    showBitrateDialog = false
                    success
                },
                onValueChange = {
                    it.toIntOrNull() != null && it.toInt() > 0
                },
                onDismiss = {
                    showBitrateDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = "Bitrate",
            iconResId = R.drawable.edit,
            onClick = {
                showBitrateDialog = true
            }
        )
    }
}

@Composable
fun VideoEditorAdjustContent(
    modifications: SnapshotStateList<VideoModification>,
    modifier: Modifier = Modifier,
    increaseModCount: () -> Unit
) {
    LazyRow(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(
            count = MediaAdjustments.entries.size
        ) { index ->
            val entry = MediaAdjustments.entries[index]

            VideoEditingAdjustmentItem(
                type = entry,
                modifications = modifications,
                extraOnClick = increaseModCount
            )
        }
    }
}

@Composable
private fun VideoEditingAdjustmentItem(
    type: MediaAdjustments,
    modifications: SnapshotStateList<VideoModification>,
    extraOnClick: () -> Unit
) {
    EditingViewBottomAppBarItem(
        text = stringResource(id = type.title),
        iconResId = type.icon,
        selected = (modifications.lastOrNull {
            it is VideoModification.Adjustment
        } as? VideoModification.Adjustment)?.type == type,
        onClick = {
            val last = modifications.lastOrNull {
                it is VideoModification.Adjustment && it.type == type
            } as? VideoModification.Adjustment

            if (last != null) {
                val latest = modifications.lastOrNull { it is VideoModification.Adjustment } as? VideoModification.Adjustment

                if (latest?.type == type) { // double click for reset value
                    modifications.remove(last)
                    modifications.add(last.copy(value = type.startValue))
                } else {
                    modifications.remove(last) // single click for switch
                    modifications.add(last)
                }
            } else {
                modifications.add(
                    VideoModification.Adjustment(
                        type = type,
                        value = type.startValue
                    )
                )
            }

            extraOnClick()
        }
    )
}