package dev.nalamzap.comig.feature.reader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nalamzap.comig.core.util.ShareUtils
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
    private var currentComicUri: Uri? = null

    fun loadComic(uri: Uri) {
        currentComicUri = uri
        viewModelScope.launch {
            try {
                val comicId = uri.path ?: uri.toString()
                currentComicId = comicId

                val comic = repository.getComic(comicId)
                val pages = repository.getPages(uri)

                repository.updateLastOpened(comicId)

                val initialPage = comic?.lastReadPage ?: 0
                val items = buildReaderItems(pages)
                val initialItemIndex = mapPageToItemIndex(items, initialPage)

                _state.value = ReaderState(
                    pages = pages,
                    readerItems = items,
                    currentPage = initialPage,
                    currentReaderItemIndex = initialItemIndex,
                    readingDirection = comic?.readingDirection ?: ReadingDirection.RIGHT_TO_LEFT,
                    title = comic?.title ?: ""
                )

                // Observe bookmarks for this comic
                launch {
                    repository.observeBookmarksForComic(comicId).collect { bookmarks ->
                        val isBookmarked = bookmarks.any { it.pageIndex == _state.value.currentPage }
                        _state.value = _state.value.copy(
                            bookmarks = bookmarks,
                            isCurrentPageBookmarked = isBookmarked
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildReaderItems(pages: List<ComicPage>): List<ReaderPageItem> {
        if (pages.isEmpty()) return emptyList()
        val items = mutableListOf<ReaderPageItem>()
        var adCount = 0

        pages.forEachIndexed { index, page ->
            items.add(ReaderPageItem.Page(page, index))
            // Insert full-page native ad every 10 pages (after page 9, 19, 29, etc.), but not after the last page
            if ((index + 1) % 10 == 0 && index != pages.lastIndex) {
                items.add(ReaderPageItem.Ad(adCount++))
            }
        }
        return items
    }

    fun mapPageToItemIndex(items: List<ReaderPageItem>, pageIndex: Int): Int {
        val index = items.indexOfFirst { it is ReaderPageItem.Page && it.pageIndex == pageIndex }
        return if (index >= 0) index else 0
    }

    fun onReaderItemChanged(itemIndex: Int) {
        val items = _state.value.readerItems
        if (itemIndex in items.indices) {
            val item = items[itemIndex]
            if (item is ReaderPageItem.Page) {
                val page = item.pageIndex
                val isBookmarked = _state.value.bookmarks.any { it.pageIndex == page }
                _state.value = _state.value.copy(
                    currentPage = page,
                    currentReaderItemIndex = itemIndex,
                    isCurrentPageBookmarked = isBookmarked
                )
                currentComicId?.let { id ->
                    viewModelScope.launch {
                        repository.updateLastReadPage(id, page)
                    }
                }
            } else {
                _state.value = _state.value.copy(currentReaderItemIndex = itemIndex)
            }
        }
    }

    fun onPageChanged(page: Int) {
        val items = _state.value.readerItems
        val itemIndex = mapPageToItemIndex(items, page)
        onReaderItemChanged(itemIndex)
    }

    fun toggleBookmark(note: String? = null) {
        val comicId = currentComicId ?: return
        val page = _state.value.currentPage
        val comicTitle = _state.value.title
        val isBookmarked = _state.value.isCurrentPageBookmarked

        viewModelScope.launch {
            if (isBookmarked) {
                repository.removeBookmark(comicId, page)
            } else {
                val pages = _state.value.pages
                val coverPath = if (page in pages.indices) pages[page].entryName else null
                repository.addBookmark(
                    comicId = comicId,
                    comicTitle = comicTitle,
                    pageIndex = page,
                    note = note,
                    coverPath = coverPath
                )
            }
        }
    }

    fun removeBookmark(pageIndex: Int) {
        val comicId = currentComicId ?: return
        viewModelScope.launch {
            repository.removeBookmark(comicId, pageIndex)
        }
    }

    fun setShowBookmarksSheet(show: Boolean) {
        _state.value = _state.value.copy(showBookmarksSheet = show)
    }

    fun setShowShareDialog(show: Boolean) {
        _state.value = _state.value.copy(showShareDialog = show)
    }

    fun shareCurrentPage(context: Context, caption: String?, screenWidth: Int) {
        val uri = currentComicUri ?: return
        val pages = _state.value.pages
        val currentPage = _state.value.currentPage
        if (currentPage !in pages.indices) return

        viewModelScope.launch {
            try {
                val page = pages[currentPage]
                val bitmap = repository.loadPageBitmap(uri, page, screenWidth)
                ShareUtils.shareComicPage(context, bitmap, caption)
            } catch (e: Exception) {
                e.printStackTrace()
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
