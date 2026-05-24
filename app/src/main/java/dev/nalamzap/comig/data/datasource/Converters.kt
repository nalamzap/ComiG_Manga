package dev.nalamzap.comig.data.datasource

import androidx.room.TypeConverter
import dev.nalamzap.comig.domain.model.Genre
import dev.nalamzap.comig.domain.model.ReadingDirection

class Converters {
    @TypeConverter
    fun fromReadingDirection(value: ReadingDirection): String {
        return value.name
    }

    @TypeConverter
    fun toReadingDirection(value: String): ReadingDirection {
        return runCatching { ReadingDirection.valueOf(value) }
            .getOrDefault(ReadingDirection.RIGHT_TO_LEFT)
    }

    @TypeConverter
    fun fromGenreList(value: List<Genre>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toGenreList(value: String): List<Genre> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { Genre.valueOf(it) }
    }
}
