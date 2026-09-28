package dev.nalamzap.comig.domain.model

data class Bookmark(
    val id: String,
    val comicId: String,
    val comicTitle: String,
    val pageIndex: Int,
    val note: String? = null,
    val coverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
