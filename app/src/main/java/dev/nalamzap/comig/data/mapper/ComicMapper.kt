package dev.nalamzap.comig.data.mapper

import android.net.Uri
import dev.nalamzap.comig.data.model.ComicEntity
import dev.nalamzap.comig.data.model.SeriesEntity
import dev.nalamzap.comig.domain.model.Comic
import dev.nalamzap.comig.domain.model.Series
import java.io.File

fun ComicEntity.toDomain(): Comic {
    return Comic(
        id = id,
        title = title,
        uri = Uri.fromFile(File(filePath)),
        pageCount = pageCount,
        lastReadPage = lastReadPage,
        readingDirection = readingDirection,
        coverPath = coverPath,
        seriesId = seriesId,
        volumeNumber = volumeNumber,
        genres = genres,
        lastOpenedAt = lastOpenedAt,
        addedAt = addedAt
    )
}

fun Comic.toEntity(): ComicEntity {
    return ComicEntity(
        id = id,
        title = title,
        filePath = uri.path ?: "",
        coverPath = coverPath ?: "",
        pageCount = pageCount,
        lastReadPage = lastReadPage,
        readingDirection = readingDirection,
        seriesId = seriesId,
        volumeNumber = volumeNumber,
        genres = genres,
        lastOpenedAt = lastOpenedAt,
        addedAt = addedAt
    )
}

fun SeriesEntity.toDomain(): Series {
    return Series(
        id = id,
        title = title,
        coverPath = coverPath,
        genres = genres,
        addedAt = addedAt
    )
}

fun Series.toEntity(): SeriesEntity {
    return SeriesEntity(
        id = id,
        title = title,
        coverPath = coverPath,
        genres = genres,
        addedAt = addedAt
    )
}
