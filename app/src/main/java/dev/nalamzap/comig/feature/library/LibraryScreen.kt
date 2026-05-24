package dev.nalamzap.comig.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.nalamzap.comig.domain.model.Comic
import dev.nalamzap.comig.domain.model.Genre
import dev.nalamzap.comig.domain.model.Series

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onImportClick: () -> Unit,
    onComicClick: (Uri) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val seriesTitles by viewModel.seriesTitles.collectAsState()
    val allSeries by viewModel.series.collectAsState()
    var showImportBSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    
    var selectedComicForSeries by remember { mutableStateOf<Comic?>(null) }
    var selectedComicForGenres by remember { mutableStateOf<Comic?>(null) }
    var selectedSeriesForManagement by remember { mutableStateOf<String?>(null) }
    var selectedSeriesForDetails by remember { mutableStateOf<Series?>(null) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val filteredComics = remember(state.comics, state.searchQuery, state.sortBy) {
        state.comics.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
            .sortedWith { a, b ->
                when (state.sortBy) {
                    SortBy.TITLE -> a.title.compareTo(b.title)
                    SortBy.ADDED -> b.addedAt.compareTo(a.addedAt)
                }
            }
    }

    val groupedItems = remember(filteredComics, state.groupingMode, allSeries) {
        when (state.groupingMode) {
            GroupingMode.SERIES -> {
                filteredComics.groupBy { it.seriesId ?: "Others" }.toSortedMap().mapValues { (_, comics) ->
                    comics.map { LibraryDisplayItem.SingleComic(it) }
                }
            }
            GroupingMode.GENRE -> {
                val genreMap = mutableMapOf<String, MutableList<LibraryDisplayItem>>()
                
                // Track which comics are already in a series to avoid duplicates in Genre view
                val comicsInSeries = mutableSetOf<String>()

                // Add Series to Genre groups
                allSeries.forEach { series ->
                    val seriesComics = filteredComics.filter { it.seriesId == series.id }
                    if (seriesComics.isNotEmpty()) {
                        comicsInSeries.addAll(seriesComics.map { it.id })
                        val item = LibraryDisplayItem.SeriesItem(series, seriesComics)
                        if (series.genres.isEmpty()) {
                            genreMap.getOrPut("Uncategorized") { mutableListOf() }.add(item)
                        } else {
                            series.genres.forEach { genre ->
                                genreMap.getOrPut(genre.name) { mutableListOf() }.add(item)
                            }
                        }
                    }
                }

                // Add Standalone comics to genre groups
                filteredComics.filter { it.id !in comicsInSeries }.forEach { comic ->
                    val item = LibraryDisplayItem.SingleComic(comic)
                    if (comic.genres.isEmpty()) {
                        genreMap.getOrPut("Uncategorized") { mutableListOf() }.add(item)
                    } else {
                        comic.genres.forEach { genre ->
                            genreMap.getOrPut(genre.name) { mutableListOf() }.add(item)
                        }
                    }
                }
                
                genreMap.toSortedMap()
            }
            GroupingMode.NONE -> emptyMap()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "ComiG Manga",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    actions = {
                        var showGroupingMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showGroupingMenu = true }) {
                                Icon(
                                    imageVector = when (state.groupingMode) {
                                        GroupingMode.NONE -> Icons.Default.Book
                                        GroupingMode.SERIES -> Icons.AutoMirrored.Outlined.LibraryBooks
                                        GroupingMode.GENRE -> Icons.Default.FilterList
                                    },
                                    contentDescription = "Grouping Mode",
                                    tint = if (state.groupingMode != GroupingMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showGroupingMenu,
                                onDismissRequest = { showGroupingMenu = false }
                            ) {
                                GroupingMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(when(mode) {
                                                GroupingMode.NONE -> "No Grouping"
                                                GroupingMode.SERIES -> "Group by Series"
                                                GroupingMode.GENRE -> "Group by Genre"
                                            })
                                        },
                                        onClick = {
                                            viewModel.onGroupingModeChange(mode)
                                            showGroupingMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.groupingMode == mode) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )

                AnimatedVisibility(visible = isSearchActive) {
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        sortBy = state.sortBy,
                        onSortChange = viewModel::onSortByChange
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showImportBSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Import") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotRadius = 1.dp.toPx()
                val spacing = 24.dp.toPx()
                for (x in 0..size.width.toInt() step spacing.toInt()) {
                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.05f),
                            radius = dotRadius,
                            center = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(padding)) {
                AnimatedVisibility(
                    visible = state.comics.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EmptyLibraryState(onImportClick = { showImportBSheet = true })
                }

                AnimatedVisibility(
                    visible = state.comics.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (state.groupingMode != GroupingMode.NONE) {
                            groupedItems.forEach { (header, items) ->
                                item {
                                    Column {
                                        Text(
                                            text = if (header == "Others") "Single Volumes" else header,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(bottom = 12.dp)
                                                .clickable(enabled = header != "Others" && state.groupingMode == GroupingMode.SERIES) {
                                                    selectedSeriesForManagement = header
                                                }
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            items(items) { item ->
                                                when (item) {
                                                    is LibraryDisplayItem.SingleComic -> {
                                                        ComicGridItem(
                                                            comic = item.comic,
                                                            onClick = { onComicClick(item.comic.uri) },
                                                            onLongClick = { selectedComicForSeries = item.comic }
                                                        )
                                                    }
                                                    is LibraryDisplayItem.SeriesItem -> {
                                                        SeriesGridItem(
                                                            series = item.series,
                                                            comics = item.comics,
                                                            onClick = { selectedSeriesForDetails = item.series },
                                                            onLongClick = { selectedSeriesForManagement = item.series.id }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val columns = 3
                            val rows = (filteredComics.size + columns - 1) / columns
                            for (i in 0 until rows) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        for (j in 0 until columns) {
                                            val index = i * columns + j
                                            if (index < filteredComics.size) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    ComicGridItem(
                                                        comic = filteredComics[index],
                                                        onClick = { onComicClick(filteredComics[index].uri) },
                                                        onLongClick = { selectedComicForSeries = filteredComics[index] }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showImportBSheet) {
                ImportBottomSheet(
                    onDismiss = { showImportBSheet = false },
                    onImport = { uri ->
                        viewModel.importComic(uri)
                        showImportBSheet = false
                    },
                    onImportFolder = { uri ->
                        viewModel.importFolder(uri)
                        showImportBSheet = false
                    }
                )
            }

            selectedComicForSeries?.let { comic ->
                SeriesSelectionDialog(
                    title = "Manage: ${comic.title}",
                    currentSeries = comic.seriesId,
                    existingSeries = seriesTitles,
                    onDismiss = { selectedComicForSeries = null },
                    onSeriesSelected = { newSeries ->
                        viewModel.updateSeries(comic.id, newSeries)
                        selectedComicForSeries = null
                    },
                    onOpenGenreSelection = {
                        selectedComicForGenres = selectedComicForSeries
                        selectedComicForSeries = null
                    },
                    onDelete = {
                        selectedComicForSeries?.let { viewModel.deleteComic(it.id) }
                        selectedComicForSeries = null
                    }
                )
            }

            selectedComicForGenres?.let { comic ->
                GenreSelectionDialog(
                    title = "Edit Genres",
                    currentGenres = comic.genres,
                    onDismiss = { selectedComicForGenres = null },
                    onGenresSelected = { newGenres ->
                        viewModel.updateGenres(comic.id, newGenres)
                        selectedComicForGenres = null
                    }
                )
            }

            selectedSeriesForManagement?.let { seriesId ->
                val series = allSeries.find { it.id == seriesId }
                val seriesComics = state.comics.filter { it.seriesId == seriesId }
                
                SeriesManagementDialog(
                    series = series ?: Series(id = seriesId, title = seriesId),
                    comics = seriesComics,
                    onDismiss = { selectedSeriesForManagement = null },
                    onGenresSelected = { genres ->
                        viewModel.updateSeriesGenres(seriesId, genres)
                    },
                    onCoverSelected = { coverPath ->
                        viewModel.updateSeriesCover(seriesId, coverPath)
                    },
                    onDeleteSeries = { deleteFiles ->
                        viewModel.deleteSeries(seriesId, deleteFiles)
                    }
                )
            }

            selectedSeriesForDetails?.let { series ->
                val seriesComics = state.comics.filter { it.seriesId == series.id }
                SeriesDetailsBottomSheet(
                    series = series,
                    comics = seriesComics,
                    onDismiss = { selectedSeriesForDetails = null },
                    onComicClick = { uri ->
                        onComicClick(uri)
                        selectedSeriesForDetails = null
                    }
                )
            }
        }
    }
}

sealed class LibraryDisplayItem {
    data class SingleComic(val comic: Comic) : LibraryDisplayItem()
    data class SeriesItem(val series: Series, val comics: List<Comic>) : LibraryDisplayItem()
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortBy: SortBy,
    onSortChange: (SortBy) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search your comics...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))

            Box {
                TextButton(
                    onClick = { showSortMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = when(sortBy) {
                            SortBy.TITLE -> "Title"
                            SortBy.ADDED -> "Added"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortBy.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onSortChange(option)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortBy == option) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Your library is empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Start by importing your favorite comics and manga to build your digital collection.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Import First Comic", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SeriesGridItem(
    series: Series,
    comics: List<Comic>,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val coverPath = series.coverPath ?: comics.firstOrNull()?.coverPath
                AsyncImage(
                    model = coverPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Stack effect for series
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${comics.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (coverPath == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.LibraryBooks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = series.title,
            maxLines = 2,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = MaterialTheme.typography.labelLarge.lineHeight * 0.9f
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailsBottomSheet(
    series: Series,
    comics: List<Comic>,
    onDismiss: () -> Unit,
    onComicClick: (Uri) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .size(80.dp, 120.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AsyncImage(
                        model = series.coverPath ?: comics.firstOrNull()?.coverPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${comics.size} Volumes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (series.genres.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            series.genres.take(3).forEach { genre ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = genre.name.lowercase().replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Volumes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(comics.sortedBy { it.volumeNumber ?: 0 }) { comic ->
                    ListItem(
                        headlineContent = { Text(comic.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            if (comic.pageCount > 0) {
                                Text("${comic.pageCount} pages")
                            }
                        },
                        leadingContent = {
                            AsyncImage(
                                model = comic.coverPath,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp, 60.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        },
                        trailingContent = {
                            if (comic.pageCount > 0 && comic.lastReadPage > 0) {
                                val progress = (comic.lastReadPage + 1).toFloat() / comic.pageCount.toFloat()
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.clickable { onComicClick(comic.uri) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicGridItem(
    comic: Comic,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = comic.coverPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (comic.pageCount > 0 && comic.lastReadPage > 0) {
                    val progress = (comic.lastReadPage + 1).toFloat() / comic.pageCount.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )
                    
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "${comic.lastReadPage + 1}/${comic.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                if (comic.coverPath == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = comic.title,
            maxLines = 2,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = MaterialTheme.typography.labelLarge.lineHeight * 0.9f
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SeriesSelectionDialog(
    title: String,
    currentSeries: String?,
    existingSeries: List<String>,
    onDismiss: () -> Unit,
    onSeriesSelected: (String?) -> Unit,
    onOpenGenreSelection: () -> Unit,
    onDelete: () -> Unit
) {
    var newSeriesName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenGenreSelection,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Edit Genres")
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete Volume")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (currentSeries != null) {
                    TextButton(
                        onClick = { onSeriesSelected(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove from Series ($currentSeries)")
                    }
                }
                
                if (existingSeries.isNotEmpty()) {
                    Text("Existing Series:", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(existingSeries) { series ->
                            if (series != currentSeries) {
                                DropdownMenuItem(
                                    text = { Text(series) },
                                    onClick = { onSeriesSelected(series) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Move to Series:", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = newSeriesName,
                    onValueChange = { newSeriesName = it },
                    placeholder = { Text("Series Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newSeriesName.isNotBlank()) onSeriesSelected(newSeriesName) },
                enabled = newSeriesName.isNotBlank()
            ) {
                Text("Create & Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GenreSelectionDialog(
    title: String,
    currentGenres: List<Genre>,
    onDismiss: () -> Unit,
    onGenresSelected: (List<Genre>) -> Unit
) {
    val selectedGenres = remember { mutableStateListOf(*currentGenres.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(Genre.entries) { genre ->
                    val isSelected = selectedGenres.contains(genre)
                    ListItem(
                        headlineContent = { Text(genre.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedGenres.add(genre)
                                    else selectedGenres.remove(genre)
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            if (isSelected) selectedGenres.remove(genre)
                            else selectedGenres.add(genre)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onGenresSelected(selectedGenres.toList()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SeriesManagementDialog(
    series: Series,
    comics: List<Comic>,
    onDismiss: () -> Unit,
    onGenresSelected: (List<Genre>) -> Unit,
    onCoverSelected: (String?) -> Unit,
    onDeleteSeries: (Boolean) -> Unit
) {
    var showGenreDialog by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Series: ${series.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showGenreDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Series Genres")
                }

                Button(
                    onClick = { showCoverDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Series Cover")
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete Series")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Series") },
            text = { Text("Do you want to delete only the series grouping or also delete all comic files in this series?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSeries(true)
                        showDeleteDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Delete All Files")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteSeries(false)
                        showDeleteDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Only Unlink")
                }
            }
        )
    }

    if (showGenreDialog) {
        GenreSelectionDialog(
            title = "Series Genres",
            currentGenres = series.genres,
            onDismiss = { showGenreDialog = false },
            onGenresSelected = {
                onGenresSelected(it)
                showGenreDialog = false
            }
        )
    }

    if (showCoverDialog) {
        AlertDialog(
            onDismissRequest = { showCoverDialog = false },
            title = { Text("Select Volume for Cover") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(comics) { comic ->
                        ListItem(
                            headlineContent = { Text(comic.title) },
                            leadingContent = {
                                AsyncImage(
                                    model = comic.coverPath,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            trailingContent = {
                                if (series.coverPath == comic.coverPath) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable {
                                onCoverSelected(comic.coverPath)
                                showCoverDialog = false
                            }
                        )
                    }
                    item {
                        ListItem(
                            headlineContent = { Text("Default (First Volume)") },
                            modifier = Modifier.clickable {
                                onCoverSelected(null)
                                showCoverDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBottomSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onImport: (Uri) -> Unit = {},
    onImportFolder: (Uri) -> Unit = {}
) {
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        onImport.invoke(uri)
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        onImportFolder.invoke(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Import Comic",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            ImportOptionItem(
                title = "Local .cbz / .zip",
                subtitle = "Import a Comic Book Zip file",
                icon = Icons.Default.Description,
                onClick = { fileLauncher.launch(arrayOf("application/zip", "application/x-cbz")) }
            )
            ImportOptionItem(
                title = "Local .pdf",
                subtitle = "Import a PDF document",
                icon = Icons.Default.PictureAsPdf,
                onClick = { fileLauncher.launch(arrayOf("application/pdf")) }
            )
            ImportOptionItem(
                title = "Local folder",
                subtitle = "Import a folder of images",
                icon = Icons.Default.Folder,
                onClick = { folderLauncher.launch(null) }
            )
        }
    }
}

@Composable
fun ImportOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
