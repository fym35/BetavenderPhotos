package com.kaii.bphotos.compose.single_photo.editing_view.video_editor

import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.kaii.bphotos.R
import com.kaii.bphotos.compose.dialogs.LavenderDialogBase
import com.kaii.bphotos.helpers.AnimationConstants
import com.kaii.bphotos.helpers.TextStylingConstants
import com.kaii.bphotos.helpers.editing.DrawingPaintState
import com.kaii.bphotos.helpers.editing.MediaColorFilters
import com.kaii.bphotos.helpers.editing.VideoModification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Ported from upstream VideoPlayerStuff.kt VideoPlayerSeekbar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerSeekbar(
    currentPosition: Float,
    duration: Float,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    onValueChange: (position: Float) -> Unit
) {
    val localInteractionSource = remember { MutableInteractionSource() }
    val isDraggingSlider by localInteractionSource.collectIsDraggedAsState()

    val animatedPosition by animateFloatAsState(
        targetValue = currentPosition,
        animationSpec =
            if (isDraggingSlider) snap()
            else tween(
                durationMillis = AnimationConstants.DURATION,
                easing = LinearEasing
            )
    )

    Slider(
        value = animatedPosition,
        valueRange = 0f..duration,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = localInteractionSource,
                thumbSize = DpSize(6.dp, 20.dp),
            )
        },
        track = { sliderState ->
            val colors = SliderDefaults.colors()

            SliderDefaults.Track(
                sliderState = sliderState,
                trackInsideCornerSize = 8.dp,
                colors = colors.copy(
                    activeTickColor = colors.activeTrackColor,
                    inactiveTickColor = colors.inactiveTrackColor,
                    disabledActiveTickColor = colors.disabledActiveTrackColor,
                    disabledInactiveTickColor = colors.disabledInactiveTrackColor,

                    activeTrackColor = colors.activeTrackColor,
                    inactiveTrackColor = colors.inactiveTrackColor,

                    disabledThumbColor = colors.activeTrackColor,
                    thumbColor = colors.activeTrackColor
                ),
                thumbTrackGapSize = 4.dp,
                drawTick = { _, _ -> },
                modifier = Modifier
                    .height(20.dp)
            )
        },
        interactionSource = localInteractionSource,
        modifier = modifier
            .height(32.dp)
    )
}

/**
 * Simplified stand-in for upstream ColorRangeSlider/PopupPillSlider in the video editor.
 * Betavender's versions have fixed ranges and no confirm callback, so this plain
 * slider preserves the upstream live-update + on-release-commit behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoAdjustSlider(
    value: MutableFloatState,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChange: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    Slider(
        value = value.floatValue.coerceIn(range.start, range.endInclusive),
        valueRange = range,
        enabled = enabled,
        onValueChange = {
            value.floatValue = it
            onValueChange()
        },
        onValueChangeFinished = onConfirm,
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                drawTick = { _, _ -> },
                modifier = Modifier.height(32.dp)
            )
        },
        modifier = modifier
    )
}

/** Ported from upstream UserActionDialogs.kt SliderDialog (missing in Betavender). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderDialog(
    title: (Float) -> String,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    startsAt: Float = 1f,
    onSetValue: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(startsAt) }

    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Text(
            text = title(sliderValue),
            fontSize = TextUnit(TextStylingConstants.LARGE_TEXT_SIZE, TextUnitType.Sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            valueRange = range,
            steps = steps,
            onValueChange = {
                sliderValue = it
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    drawTick = { _, _ -> },
                    modifier = Modifier
                        .height(32.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                alignment = Alignment.End,
                space = 8.dp
            )
        ) {
            FilledTonalButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(id = R.string.media_cancel),
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                )
            }

            Button(
                onClick = {
                    onSetValue(sliderValue)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(id = R.string.media_confirm),
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                )
            }
        }
    }
}

/** Ported from upstream Miscellaneous.kt shimmerEffect (missing in Betavender). */
fun Modifier.shimmerEffect(
    containerColor: Color = Color.DarkGray,
    highlightColor: Color = Color.Gray,
    durationMillis: Int = AnimationConstants.DURATION_EXTRA_LONG,
    delayMillis: Int = 0
) = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition()
    val startOffset by transition.animateFloat(
        initialValue = -3f * size.width,
        targetValue = 3f * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis
            )
        )
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                containerColor,
                highlightColor,
                containerColor
            ),
            start = Offset(startOffset, 0f),
            end = Offset(startOffset + size.width, size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun FilterShowcase(
    image: ImageBitmap?,
    filter: MediaColorFilters,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = image != null,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = AnimationConstants.DURATION_EXTRA_LONG
                    )
                ).togetherWith(
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = AnimationConstants.DURATION_EXTRA_LONG
                        )
                    )
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(1f)
                .clip(RoundedCornerShape(12.dp))
        ) { state ->
            if (state) {
                Image(
                    bitmap = image!!,
                    contentDescription = stringResource(id = filter.title),
                    colorFilter = ColorFilter.colorMatrix(filter.matrix),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = filter.title),
            fontSize = TextUnit(TextStylingConstants.EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = filter.tag),
                fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                textAlign = TextAlign.Start
            )

            Text(
                text = stringResource(id = filter.description),
                fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Simplified port of upstream VideoFilterPage.
 * Upstream composited live MediaAdjustments onto the preview thumbnail; Betavender's
 * MediaAdjustments.getMatrix is not accessible (private effect interface), so the
 * thumbnail shows the raw frame. The live player preview still shows adjustments
 * via ExoPlayer effects, and filter selection behavior is unchanged.
 */
@Composable
fun VideoFilterPage(
    drawingPaintState: DrawingPaintState,
    currentVideoPosition: MutableFloatState,
    absolutePath: String,
    allowedToRefresh: Boolean,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    var bitmap: ImageBitmap? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentVideoPosition.floatValue, allowedToRefresh) {
        if (allowedToRefresh) return@LaunchedEffect

        coroutineScope.launch(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(absolutePath)

                var localBitmap = ImageBitmap(8, 8)

                metadata.getFrameAtTime((currentVideoPosition.floatValue * 1000 * 1000).toLong())?.let {
                    localBitmap = it.asImageBitmap()
                }

                bitmap = localBitmap
            } catch (_: Exception) {
            } finally {
                try {
                    metadata.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        drawingPaintState.modifications.removeAll {
            it is VideoModification.Filter
        }
        drawingPaintState.modifications.add(
            VideoModification.Filter(
                type = MediaColorFilters.entries[pagerState.currentPage]
            )
        )
    }

    FilterPager(
        bitmap = bitmap,
        pagerState = pagerState,
        modifier = modifier
    )
}

@Composable
private fun FilterPager(
    bitmap: ImageBitmap?,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(6)
        ),
        modifier = modifier
    ) { index ->
        FilterShowcase(
            image = bitmap,
            filter = MediaColorFilters.entries[index]
        )
    }
}
