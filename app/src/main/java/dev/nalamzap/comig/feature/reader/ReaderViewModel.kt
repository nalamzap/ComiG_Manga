package dev.nalamzap.comig.feature.reader

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nalamzap.comig.data.repository.ComicRepositoryImpl
import dev.nalamzap.comig.domain.model.ComicPage
import dev.nalamzap.comig.domain.model.ReadingDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repository = ComicRepositoryImpl(app)

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state

    private var currentComicId: String? = null

    fun loadComic(uri: Uri) {
        viewModelScope.launch {
            try {
                val comicId = uri.path ?: uri.toString()
                currentComicId = comicId
                
                val comic = repository.getComic(comicId)
                val pages = repository.getPages(uri)
                
                repository.updateLastOpened(comicId)
                
                _state.value = ReaderState(
                    pages = pages,
                    currentPage = comic?.lastReadPage ?: 0,
                    readingDirection = comic?.readingDirection ?: ReadingDirection.RIGHT_TO_LEFT,
                    title = comic?.title ?: ""
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error state if needed
            }
        }
    }

    fun onPageChanged(page: Int) {
        _state.value = _state.value.copy(currentPage = page)
        currentComicId?.let { id ->
            viewModelScope.launch {
                repository.updateLastReadPage(id, page)
            }
        }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        _state.value = _state.value.copy(readingDirection = direction)
        currentComicId?.let { id ->
            viewModelScope.launch {
                repository.updateReadingDirection(id, direction)
            }
        }
    }

    suspend fun loadPage(
        uri: Uri,
        page: ComicPage,
        width: Int
    ): Bitmap =
        repository.loadPageBitmap(
            uri,
            page,
            width
        )
}
