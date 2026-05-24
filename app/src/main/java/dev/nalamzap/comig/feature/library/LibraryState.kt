package dev.nalamzap.comig.feature.library

import dev.nalamzap.comig.domain.model.Comic

data class LibraryState(
    val comics: List<Comic> = emptyList(),
    val groupingMode: GroupingMode = GroupingMode.SERIES,
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.TITLE
)

enum class GroupingMode {
    NONE, SERIES, GENRE
}

enum class SortBy {
    TITLE, ADDED
}
