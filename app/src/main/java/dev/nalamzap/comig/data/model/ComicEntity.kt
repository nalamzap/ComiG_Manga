package dev.nalamzap.comig.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.nalamzap.comig.domain.model.Genre
import dev.nalamzap.comig.domain.model.ReadingDirection

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val coverPath: String,
    val pageCount: Int,
    val lastReadPage: Int = 0,
    val readingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val seriesId: String? = null,
    val volumeNumber: Int? = null,
    val genres: List<Genre> = emptyList(),
    val lastOpenedAt: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val coverPath: String? = null,
    val genres: List<Genre> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)
