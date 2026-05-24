package dev.nalamzap.comig.data.datasource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nalamzap.comig.data.model.ComicEntity
import dev.nalamzap.comig.data.model.SeriesEntity
import dev.nalamzap.comig.domain.model.ReadingDirection
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {

    @Query("SELECT * FROM comics ORDER BY addedAt DESC")
    fun observeComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getComicById(id: String): ComicEntity?

    @Query("SELECT * FROM comics WHERE lastOpenedAt > 0 ORDER BY lastOpenedAt DESC LIMIT 10")
    fun observeRecentlyRead(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM series ORDER BY title ASC")
    fun observeSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getSeriesById(id: String): SeriesEntity?

    @Query("SELECT * FROM comics WHERE seriesId = :seriesId ORDER BY volumeNumber ASC")
    fun observeComicsBySeries(seriesId: String): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE seriesId = :seriesId")
    suspend fun getComicsBySeries(seriesId: String): List<ComicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comic: ComicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: SeriesEntity)

    @Query("UPDATE series SET genres = :genres WHERE id = :id")
    suspend fun updateSeriesGenres(id: String, genres: List<dev.nalamzap.comig.domain.model.Genre>)

    @Query("UPDATE series SET coverPath = :coverPath WHERE id = :id")
    suspend fun updateSeriesCover(id: String, coverPath: String?)

    @Query("UPDATE comics SET lastReadPage = :page WHERE id = :id")
    suspend fun updateLastReadPage(id: String, page: Int)

    @Query("UPDATE comics SET readingDirection = :direction WHERE id = :id")
    suspend fun updateReadingDirection(id: String, direction: ReadingDirection)

    @Query("UPDATE comics SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE comics SET coverPath = :coverPath WHERE id = :id")
    suspend fun updateCoverPath(id: String, coverPath: String)

    @Query("UPDATE comics SET seriesId = :seriesId WHERE id = :id")
    suspend fun updateSeries(id: String, seriesId: String?)

    @Query("UPDATE comics SET genres = :genres WHERE id = :id")
    suspend fun updateGenres(id: String, genres: List<dev.nalamzap.comig.domain.model.Genre>)

    @Query("SELECT DISTINCT seriesId FROM comics WHERE seriesId IS NOT NULL")
    fun observeSeriesTitles(): Flow<List<String>>

    @Query("DELETE FROM series WHERE id = :seriesId")
    suspend fun deleteSeriesById(seriesId: String)

    @Query("UPDATE comics SET seriesId = NULL WHERE seriesId = :seriesId")
    suspend fun unlinkComicsFromSeries(seriesId: String)

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteComicById(id: String)

    @Delete
    suspend fun delete(comic: ComicEntity)

    @Delete
    suspend fun deleteSeries(series: SeriesEntity)
}