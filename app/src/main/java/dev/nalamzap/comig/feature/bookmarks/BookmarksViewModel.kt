package dev.nalamzap.comig.feature.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nalamzap.comig.data.repository.ComicRepositoryImpl
import dev.nalamzap.comig.domain.model.Bookmark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repository = ComicRepositoryImpl(app)

    val bookmarks: StateFlow<List<Bookmark>> = repository.observeAllBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
        }
    }
}
