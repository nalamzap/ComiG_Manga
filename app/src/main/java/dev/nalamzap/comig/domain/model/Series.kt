package dev.nalamzap.comig.domain.model

data class Series(
    val id: String,
    val title: String,
    val coverPath: String? = null,
    val genres: List<Genre> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)
