package dev.nalamzap.comig.feature.reader

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nalamzap.comig.domain.model.ComicPage
import dev.nalamzap.comig.domain.model.ReadingDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    comicUri: Uri,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val config = LocalConfiguration.current

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(comicUri) {
        viewModel.loadComic(comicUri)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showControls) {
                TopAppBar(
                    title = { Text(state.title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            Text(
                                "Reading Direction",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ReadingDirection.entries.forEach { direction ->
                                DropdownMenuItem(
                                    text = {
                                        val label = when (direction) {
                                            ReadingDirection.RIGHT_TO_LEFT -> "Horizontal (RTL)"
                                            ReadingDirection.TOP_TO_BOTTOM -> "Vertical"
                                        }
                                        Text(label)
                                    },
                                    onClick = {
                                        viewModel.setReadingDirection(direction)
                                        showMenu = false
                                    },
                                    trailingIcon = {
                                        if (state.readingDirection == direction) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = showControls) {
                ReaderBottomBar(
                    currentPage = state.currentPage,
                    pageCount = state.pages.size,
                    isRtl = state.readingDirection == ReadingDirection.RIGHT_TO_LEFT,
                    onPageChange = { page ->
                        viewModel.onPageChanged(page)
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.readingDirection) {
                ReadingDirection.TOP_TO_BOTTOM -> {
                    VerticalReader(
                        pages = state.pages,
                        comicUri = comicUri,
                        initialPage = state.currentPage,
                        screenWidth = config.screenWidthDp,
                        viewModel = viewModel,
                        onPageChanged = viewModel::onPageChanged,
                        onToggleControls = { showControls = !showControls }
                    )
                }
                else -> {
                    HorizontalReader(
                        pages = state.pages,
                        comicUri = comicUri,
                        initialPage = state.currentPage,
                        screenWidth = config.screenWidthDp,
                        viewModel = viewModel,
                        onPageChanged = viewModel::onPageChanged,
                        onToggleControls = { showControls = !showControls }
                    )
                }
            }
            
            // Fix: RTL Swipe issues by ensuring we don't overlay if hidden
            if (!showControls) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${state.currentPage + 1} / ${state.pages.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalReader(
    pages: List<ComicPage>,
    comicUri: Uri,
    initialPage: Int,
    screenWidth: Int,
    viewModel: ReaderViewModel,
    onPageChanged: (Int) -> Unit,
    onToggleControls: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size }
    )

    // Handle external page changes
    LaunchedEffect(initialPage) {
        if (pagerState.currentPage != initialPage && initialPage >= 0 && initialPage < pages.size) {
            pagerState.scrollToPage(initialPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        beyondViewportPageCount = 1
    ) { index ->
        PageItem(
            uri = comicUri,
            page = pages[index],
            width = screenWidth,
            viewModel = viewModel,
            onToggleControls = onToggleControls
        )
    }
}

@Composable
fun VerticalReader(
    pages: List<ComicPage>,
    comicUri: Uri,
    initialPage: Int,
    screenWidth: Int,
    viewModel: ReaderViewModel,
    onPageChanged: (Int) -> Unit,
    onToggleControls: () -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onPageChanged(listState.firstVisibleItemIndex)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        items(pages.size) { index ->
            PageItem(
                uri = comicUri,
                page = pages[index],
                width = screenWidth,
                viewModel = viewModel,
                onToggleControls = onToggleControls
            )
        }
    }
}

@Composable
fun PageItem(
    uri: Uri,
    page: ComicPage,
    width: Int,
    viewModel: ReaderViewModel,
    onToggleControls: () -> Unit
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(page) {
        bitmap = viewModel.loadPage(uri, page, width)
        // Reset zoom on page change
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 3f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(scale) {
                if (scale > 1f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 7f)
                        if (scale > 1f) {
                            offset += pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    pageCount: Int,
    isRtl: Boolean,
    onPageChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${if(isRtl) pageCount else (currentPage + 1)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = if (isRtl) (pageCount - 1 - currentPage).toFloat() else currentPage.toFloat(),
                    onValueChange = {
                        val newPage = if (isRtl) (pageCount - 1 - it.toInt()) else it.toInt()
                        onPageChange(newPage)
                    },
                    valueRange = 0f..maxOf(0f, (pageCount - 1).toFloat()),
                    steps = if (pageCount > 1) pageCount - 2 else 0,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )

                Text(
                    text = "${if(isRtl)(currentPage+1) else pageCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
