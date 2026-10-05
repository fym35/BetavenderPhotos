package com.kaii.bphotos.compose.single_photo.editing_view.video_editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.kaii.bphotos.LocalMainViewModel
import com.kaii.bphotos.LocalNavController
import com.kaii.bphotos.R
import com.kaii.bphotos.compose.SelectableDropDownMenuItem
import com.kaii.bphotos.compose.dialogs.ConfirmationDialog
import com.kaii.bphotos.compose.single_photo.EditingViewBottomAppBarItem
import com.kaii.bphotos.datastore.Editing
import com.kaii.bphotos.helpers.AnimationConstants
import com.kaii.bphotos.helpers.TextStylingConstants
import com.kaii.bphotos.helpers.VideoPlayerConstants
import com.kaii.bphotos.helpers.editing.BasicVideoData
import com.kaii.bphotos.helpers.editing.CroppingAspectRatio
import com.kaii.bphotos.helpers.editing.DrawingColors
import com.kaii.bphotos.helpers.editing.DrawingItems
import com.kaii.bphotos.helpers.editing.DrawingPaintState
import com.kaii.bphotos.helpers.editing.MediaColorFilters
import com.kaii.bphotos.helpers.editing.SharedModification
import com.kaii.bphotos.helpers.editing.VideoEditingState
import com.kaii.bphotos.helpers.editing.VideoEditorTabs
import com.kaii.bphotos.helpers.editing.VideoModification
import com.kaii.bphotos.helpers.editing.saveVideo
import com.kaii.bphotos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private const val TAG = "com.kaii.bphotos.compose.single_photo.editing_view.VideoEditorBars"

@Composable
fun VideoEditorBottomBar(
    pagerState: PagerState,
    currentPosition: MutableFloatState,
    basicData: BasicVideoData,
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    modifications: SnapshotStateList<VideoModification>,
    uri: Uri,
    increaseModCount: () -> Unit,
    onSeek: (Float) -> Unit,
    saveEffect: (MediaColorFilters) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val navBarHeight = with(localDensity) {
        WindowInsets.navigationBars.getBottom(localDensity).toDp()
    }

    BottomAppBar(
        modifier = Modifier
            .height(120.dp + navBarHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val context = LocalContext.current
            var availableEditors by remember {
                mutableStateOf(
                    emptyList<EditorApp>()
                )
            }

            LaunchedEffect(Unit) {
                availableEditors = getAvailableEditorsForType(
                    context = context,
                    mediaType = MediaType.Video,
                )
            }

            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(horizontal = 8.dp)
            ) {
                items(
                    count = VideoEditorTabs.entries.size,
                    key = { VideoEditorTabs.entries[it] }
                ) { tabIndex ->
                    val entry = VideoEditorTabs.entries[tabIndex]
                    if (entry != VideoEditorTabs.More || availableEditors.isNotEmpty()) {
                        val selected = pagerState.currentPage == tabIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(tabIndex)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(id = entry.title),
                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // preload thumbnails so trim tab doesn't fetch on every navigation
            val metadata = remember { MediaMetadataRetriever() }
            val thumbnails = remember { mutableStateListOf<Bitmap>() }
            val windowInfo = LocalWindowInfo.current

            LaunchedEffect(basicData) {
                Log.d(TAG, "Basic data updated $basicData")
                if (basicData.duration <= 0f) return@LaunchedEffect

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        metadata.setDataSource(basicData.absolutePath)

                        val stepSize = basicData.duration.roundToInt().seconds.inWholeMicroseconds / 6

                        for (i in 0..<VideoPlayerConstants.TRIM_THUMBNAIL_COUNT) {
                            val new = metadata.getScaledFrameAtTime(
                                stepSize * i,
                                MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
                                windowInfo.containerSize.width / (VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - 2),
                                windowInfo.containerSize.width / (VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - 2)
                            )

                            new?.let { thumbnails.add(it) }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize(1f)
            ) { index ->
                when (index) {
                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Trim) -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .padding(8.dp)
                        ) {
                            TrimContent(
                                currentPosition = currentPosition,
                                videoEditingState = videoEditingState,
                                thumbnails = thumbnails,
                                onSeek = onSeek,
                                basicData = basicData
                            )
                        }
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Crop) -> {
                        VideoEditorCropContent(
                            videoEditingState = videoEditingState
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Video) -> {
                        VideoEditorProcessingContent(
                            basicData = basicData,
                            videoEditingState = videoEditingState
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust) -> {
                        VideoEditorAdjustContent(
                            modifications = modifications,
                            increaseModCount = increaseModCount
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Filters) -> {
                        SharedEditorFilterContent(
                            modifications = drawingPaintState.modifications,
                            saveEffect = saveEffect
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw) -> {
                        SharedEditorDrawContent(
                            drawingPaintState = drawingPaintState,
                            currentTime = currentPosition.floatValue
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.More) -> {
                        SharedEditorMoreContent(
                            apps = availableEditors,
                            uri = uri
                        )
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorTopBar(
    uri: Uri,
    absolutePath: String,
    modifications: SnapshotStateList<VideoModification>,
    basicVideoData: BasicVideoData,
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    lastSavedModCount: MutableIntState,
    containerDimens: Size,
    canvasSize: Size,
    isFromOpenWithView: Boolean,
    customAlbumId: Int?
) {
    val navController = LocalNavController.current
    val mainViewModel = LocalMainViewModel.current

    TopAppBar(
        title = {},
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(8.dp, 0.dp, 0.dp, 0.dp)
            ) {
                val showDialog = remember { mutableStateOf(false) }

                if (showDialog.value) {
                    ConfirmationDialog(
                        showDialog = showDialog,
                        dialogTitle = stringResource(id = R.string.editing_discard_desc),
                        confirmButtonLabel = stringResource(id = R.string.editing_discard)
                    ) {
                        navController.popBackStack()
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        if (lastSavedModCount.intValue < modifications.size) {
                            showDialog.value = true
                        } else {
                            navController.popBackStack()
                        }
                    },
                    enabled = true,
                    modifier = Modifier
                        .height(40.dp)
                        .width(56.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(id = R.string.editing_close_desc),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        },
        actions = {
            var showDropDown by remember { mutableStateOf(false) }

            val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)
            var overwrite by remember { mutableStateOf(false) }

            LaunchedEffect(overwriteByDefault) {
                overwrite = overwriteByDefault
            }

            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = {
                    showDropDown = false
                },
                shape = RoundedCornerShape(24.dp),
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_overwrite_desc),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = overwrite
                ) {
                    overwrite = true
                    showDropDown = false
                }

                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_save),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = !overwrite
                ) {
                    overwrite = false
                    showDropDown = false
                }
            }

            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val textMeasurer = rememberTextMeasurer()
            var isSaving by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showDropDown = true }
                ) {
                    Text(
                        text = if (overwrite) stringResource(id = R.string.editing_overwrite_desc)
                        else stringResource(id = R.string.editing_save),
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        maxLines = 1
                    )
                }

                Button(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                saveVideo(
                                    context = context,
                                    modifications = modifications.toList(),
                                    videoEditingState = videoEditingState,
                                    basicVideoData = basicVideoData,
                                    uri = uri,
                                    absolutePath = absolutePath,
                                    overwrite = overwrite,
                                    containerDimens = containerDimens,
                                    canvasSize = canvasSize,
                                    textMeasurer = textMeasurer,
                                    isFromOpenWithView = isFromOpenWithView,
                                    onFailure = {}
                                )
                                lastSavedModCount.intValue = modifications.size

                                if (isFromOpenWithView) {
                                    (context as? Activity)?.finish()
                                } else {
                                    navController.popBackStack()
                                }
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.media_confirm),
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            }
        }
    )
}

/** Simplified port of upstream SharedEditorCropContent (no bottom-sheet ratio picker). */
@Composable
fun VideoEditorCropContent(
    videoEditingState: VideoEditingState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize(1f)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_rotate),
            iconResId = R.drawable.rotate_ccw
        ) {
            videoEditingState.setRotation((videoEditingState.rotation + 90f) % 360f)
        }

        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(CroppingAspectRatio.entries) { ratio ->
                val selected = videoEditingState.croppingAspectRatio == ratio
                FilledTonalButton(
                    onClick = { videoEditingState.setCroppingAspectRatio(ratio) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = stringResource(id = ratio.title),
                        fontSize = TextUnit(12f, TextUnitType.Sp),
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_discard),
            iconResId = R.drawable.reset
        ) {
            videoEditingState.setRotation(0f)
            videoEditingState.setCroppingAspectRatio(CroppingAspectRatio.FreeForm)
            videoEditingState.resetCrop(true)
        }
    }
}

/** Simplified port of upstream SharedEditorFilterContent (reset + apply buttons). */
@Composable
fun SharedEditorFilterContent(
    modifications: List<SharedModification>,
    modifier: Modifier = Modifier,
    saveEffect: (MediaColorFilters) -> Unit
) {
    var original by remember {
        mutableStateOf(
            (modifications.lastOrNull {
                it is SharedModification.Filter
            } as? SharedModification.Filter)?.type ?: MediaColorFilters.None
        )
    }

    val last by remember {
        derivedStateOf {
            (modifications.lastOrNull { it is SharedModification.Filter } as? SharedModification.Filter)?.type ?: MediaColorFilters.None
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        Button(
            onClick = {
                saveEffect(original)
            },
            shape = CircleShape,
            enabled = original != last,
            modifier = Modifier
                .size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.reset),
                contentDescription = "reset the chosen filter"
            )
        }

        Button(
            onClick = {
                original = last
                saveEffect(last)
            },
            shape = CircleShape,
            enabled = original != last,
            modifier = Modifier
                .height(56.dp)
        ) {
            Text(
                text = if (original != last) "Select Filter" else "Selected",
                fontSize = TextUnit(TextStylingConstants.MEDIUM_TEXT_SIZE, TextUnitType.Sp)
            )
        }
    }
}

/** Simplified port of upstream SharedEditorDrawContent (no collapsing selectors). */
@Composable
fun SharedEditorDrawContent(
    drawingPaintState: DrawingPaintState,
    currentTime: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize(1f)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        DrawingItems.entries.forEach { item ->
            EditingViewBottomAppBarItem(
                text = stringResource(id = item.title),
                iconResId = item.icon,
                selected = drawingPaintState.paintType == item
            ) {
                drawingPaintState.setPaintType(item)
            }
        }

        FilledTonalIconButton(
            onClick = {
                drawingPaintState.undoModification()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_arrow),
                contentDescription = "remove the last drawing"
            )
        }

        FilledTonalIconButton(
            onClick = {
                drawingPaintState.clearModifications()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.trash),
                contentDescription = "remove all drawings"
            )
        }

        DrawingColors.colorList.forEach { color ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable {
                        drawingPaintState.setColor(color, currentTime)
                    }
                    .then(
                        if (drawingPaintState.color == color) Modifier.graphicsLayer { scaleX = 1.2f; scaleY = 1.2f }
                        else Modifier
                    )
            )
        }

        if (drawingPaintState.paintType == DrawingItems.Text ||
            drawingPaintState.paintType == DrawingItems.Image
        ) {
            FilledTonalIconToggleButton(
                checked = drawingPaintState.recordKeyframes,
                onCheckedChange = {
                    drawingPaintState.setRecordKeyframes(
                        record = it,
                        currentTime = currentTime
                    )
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.auto_play),
                    contentDescription = "toggle ability to record keyframes"
                )
            }
        }
    }
}

/** Simplified port of upstream SharedEditorMoreContent (external editors). */
@Composable
fun SharedEditorMoreContent(
    apps: List<EditorApp>,
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyRow(
        modifier = modifier.fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        items(apps, key = { it.packageName }) { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                setDataAndType(uri, "video/*")
                                setPackage(app.packageName)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    }
                    .padding(8.dp)
            ) {
                androidx.compose.foundation.Image(
                    bitmap = app.icon,
                    contentDescription = app.name,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = app.name,
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    maxLines = 1
                )
            }
        }
    }
}

data class EditorApp(
    val icon: ImageBitmap,
    val name: String,
    val packageName: String
)

fun getAvailableEditorsForType(
    context: Context,
    mediaType: MediaType
): List<EditorApp> {
    val editIntent = Intent(Intent.ACTION_EDIT).apply {
        type =
            if (mediaType == MediaType.Image) "image/*"
            else "video/*"
    }

    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(
            editIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(editIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    return info.map {
        EditorApp(
            icon = it.activityInfo.loadIcon(context.packageManager).toBitmap(1024, 1024).asImageBitmap(),
            name = it.loadLabel(context.packageManager).toString(),
            packageName = it.activityInfo.packageName
        )
    }
}
