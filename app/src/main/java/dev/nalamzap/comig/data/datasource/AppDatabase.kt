package dev.nalamzap.comig.data.datasource

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.nalamzap.comig.data.model.ComicEntity
import dev.nalamzap.comig.data.model.SeriesEntity

@Database(entities = [ComicEntity::class, SeriesEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
}
