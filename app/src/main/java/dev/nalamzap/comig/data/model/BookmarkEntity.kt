package dev.nalamzap.comig.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val comicId: String,
    val comicTitle: String,
    val pageIndex: Int,
    val note: String? = null,
    val coverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
