package dev.nalamzap.comig.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import dev.nalamzap.comig.domain.model.Comic
import dev.nalamzap.comig.domain.model.ComicPage
import dev.nalamzap.comig.domain.model.Genre
import dev.nalamzap.comig.domain.model.ReadingDirection
import dev.nalamzap.comig.domain.model.Series
import kotlinx.coroutines.flow.Flow

interface ComicRepository {
    suspend fun loadComic(uri: Uri): Comic
    suspend fun getPages(uri: Uri): List<ComicPage>
    suspend fun loadPageBitmap(
        uri: Uri,
        page: ComicPage,
        targetWidth: Int
    ): Bitmap

    fun observeComics(): Flow<List<Comic>>
    fun observeRecentlyRead(): Flow<List<Comic>>
    suspend fun getComic(id: String): Comic?
    suspend fun updateLastReadPage(id: String, page: Int)
    suspend fun updateReadingDirection(id: String, direction: ReadingDirection)
    suspend fun updateLastOpened(id: String)
    suspend fun importComic(uri: Uri)
    suspend fun importFolder(uri: Uri)
    suspend fun syncLocalFiles()
    suspend fun generateCover(id: String): String?
    suspend fun updateSeries(id: String, seriesId: String?)
    suspend fun updateGenres(id: String, genres: List<Genre>)
    
    fun observeSeries(): Flow<List<Series>>
    suspend fun updateSeriesGenres(id: String, genres: List<Genre>)
    suspend fun updateSeriesCover(id: String, coverPath: String?)

    suspend fun deleteComic(id: String)
    suspend fun deleteSeries(id: String, deleteComics: Boolean)

    fun observeSeriesTitles(): Flow<List<String>>
}
