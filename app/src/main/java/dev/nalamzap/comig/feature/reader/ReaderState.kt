package dev.nalamzap.comig.feature.reader

import dev.nalamzap.comig.domain.model.Bookmark
import dev.nalamzap.comig.domain.model.ComicPage
import dev.nalamzap.comig.domain.model.ReadingDirection

sealed interface ReaderPageItem {
    data class Page(val comicPage: ComicPage, val pageIndex: Int) : ReaderPageItem
    data class Ad(val adIndex: Int) : ReaderPageItem
}

data class ReaderState(
    val pages: List<ComicPage> = emptyList(),
    val readerItems: List<ReaderPageItem> = emptyList(),
    val currentPage: Int = 0,
    val currentReaderItemIndex: Int = 0,
    val readingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val title: String = "",
    val isCurrentPageBookmarked: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val showBookmarksSheet: Boolean = false,
    val showShareDialog: Boolean = false
)
