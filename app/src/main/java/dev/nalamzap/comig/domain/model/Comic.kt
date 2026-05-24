package dev.nalamzap.comig.domain.model

import android.net.Uri

data class Comic(
    val id: String,
    val title: String,
    val uri: Uri,
    val pageCount: Int,
    val lastReadPage: Int = 0,
    val readingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val coverPath: String? = null,
    val seriesId: String? = null,
    val volumeNumber: Int? = null,
    val genres: List<Genre> = emptyList(),
    val lastOpenedAt: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)
