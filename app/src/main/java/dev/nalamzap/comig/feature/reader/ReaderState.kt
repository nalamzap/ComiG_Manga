package dev.nalamzap.comig.feature.reader

import dev.nalamzap.comig.domain.model.ComicPage
import dev.nalamzap.comig.domain.model.ReadingDirection

data class ReaderState(
    val pages: List<ComicPage> = emptyList(),
    val currentPage: Int = 0,
    val readingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val title: String = ""
)
