package dev.nalamzap.comig.feature.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nalamzap.comig.data.repository.ComicRepositoryImpl
import dev.nalamzap.comig.domain.model.Comic
import dev.nalamzap.comig.domain.model.Genre
import dev.nalamzap.comig.domain.model.Series
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repository = ComicRepositoryImpl(app)

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = combine(
        _state,
        repository.observeComics()
    ) { state, comics ->
        state.copy(comics = comics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryState()
    )

    init {
        viewModelScope.launch {
            repository.syncLocalFiles()
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onSortByChange(sortBy: SortBy) {
        _state.update { it.copy(sortBy = sortBy) }
    }

    fun onGroupingModeChange(mode: GroupingMode) {
        _state.update { it.copy(groupingMode = mode) }
    }

    val seriesTitles: StateFlow<List<String>> = repository.observeSeriesTitles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val series: StateFlow<List<Series>> = repository.observeSeries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSeries(comicId: String, seriesName: String?) {
        viewModelScope.launch {
            repository.updateSeries(comicId, seriesName)
        }
    }

    fun updateSeriesGenres(seriesId: String, genres: List<Genre>) {
        viewModelScope.launch {
            repository.updateSeriesGenres(seriesId, genres)
        }
    }

    fun updateSeriesCover(seriesId: String, coverPath: String?) {
        viewModelScope.launch {
            repository.updateSeriesCover(seriesId, coverPath)
        }
    }

    fun deleteComic(id: String) {
        viewModelScope.launch {
            repository.deleteComic(id)
        }
    }

    fun deleteSeries(id: String, deleteComics: Boolean) {
        viewModelScope.launch {
            repository.deleteSeries(id, deleteComics)
        }
    }

    fun updateGenres(comicId: String, genres: List<Genre>) {
        viewModelScope.launch {
            repository.updateGenres(comicId, genres)
        }
    }

    fun importComic(uri: Uri) {
        viewModelScope.launch {
            repository.importComic(uri)
        }
    }

    fun importFolder(uri: Uri) {
        viewModelScope.launch {
            repository.importFolder(uri)
        }
    }
}
