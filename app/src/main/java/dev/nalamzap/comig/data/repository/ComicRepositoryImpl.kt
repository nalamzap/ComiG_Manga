package dev.nalamzap.comig.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.Room
import androidx.documentfile.provider.DocumentFile
import dev.nalamzap.comig.core.util.isImage
import dev.nalamzap.comig.core.util.sortedPages
import dev.nalamzap.comig.data.datasource.AppDatabase
import dev.nalamzap.comig.data.mapper.toDomain
import dev.nalamzap.comig.data.model.ComicEntity
import dev.nalamzap.comig.data.model.SeriesEntity
import dev.nalamzap.comig.domain.model.Comic
import dev.nalamzap.comig.domain.model.ComicPage
import dev.nalamzap.comig.domain.model.Genre
import dev.nalamzap.comig.domain.model.ReadingDirection
import dev.nalamzap.comig.domain.model.Series
import dev.nalamzap.comig.domain.repository.ComicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class ComicRepositoryImpl(
    private val context: Context
) : ComicRepository {

    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "comig.db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    private val dao = db.comicDao()

    override suspend fun loadComic(uri: Uri): Comic = withContext(Dispatchers.IO) {
        val zip = getZip(uri)
        val pages = zip.entries().toList().filter { it.isImage() }.sortedPages()

        Comic(
            id = uri.toString(),
            title = uri.lastPathSegment ?: "Comic",
            uri = uri,
            pageCount = pages.size
        )
    }

    override fun observeComics(): Flow<List<Comic>> {
        return dao.observeComics().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeRecentlyRead(): Flow<List<Comic>> {
        return dao.observeRecentlyRead().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getComic(id: String): Comic? {
        return dao.getComicById(id)?.toDomain()
    }

    override suspend fun updateLastReadPage(id: String, page: Int) {
        dao.updateLastReadPage(id, page)
    }

    override suspend fun updateReadingDirection(id: String, direction: ReadingDirection) {
        dao.updateReadingDirection(id, direction)
    }

    override suspend fun updateLastOpened(id: String) {
        dao.updateLastOpened(id)
    }

    override suspend fun importComic(uri: Uri) = withContext(Dispatchers.IO) {
        val fileName = getDisplayName(context, uri)
        val comicsDir = File(context.filesDir, "comics").apply { mkdirs() }
        val outFile = File(comicsDir, fileName)

        // Copy file
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            importFile(outFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun importFolder(uri: Uri) = withContext(Dispatchers.IO) {
        val folderName = getDisplayName(context, uri)
        val comicsDir = File(context.filesDir, "comics").apply { mkdirs() }
        val outDir = File(comicsDir, folderName).apply { mkdirs() }

        // Copy all images from document tree
        val treeUri = DocumentFile.fromTreeUri(context, uri) ?: return@withContext
        treeUri.listFiles().filter { it.isFile && it.name?.lowercase()?.let { name -> 
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
        } == true }.forEach { docFile ->
            docFile.name?.let { name ->
                val outFile = File(outDir, name)
                context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        importFile(outDir)
    }

    override suspend fun syncLocalFiles() = withContext(Dispatchers.IO) {
        val comicsDir = File(context.filesDir, "comics")
        if (!comicsDir.exists()) return@withContext

        comicsDir.listFiles()?.filter { it.isFile && (it.extension == "cbz" || it.extension == "zip") }?.forEach { file ->
            if (dao.getComicById(file.absolutePath) == null) {
                importFile(file)
            }
        }
    }

    private suspend fun importFile(file: File) {
        val coverPath = generateCover(file.absolutePath)

        val pageCount = when {
            file.isZip() -> {
                try {
                    ZipFile(file).use { zip ->
                        zip.entries().toList().filter { it.isImage() }.size
                    }
                } catch (e: Exception) { 0 }
            }
            file.isPdf() -> {
                try {
                    val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    val count = renderer.pageCount
                    renderer.close()
                    pfd.close()
                    count
                } catch (e: Exception) { 0 }
            }
            file.isDirectory -> {
                file.listFiles()?.count { it.isImageFile() } ?: 0
            }
            else -> 0
        }

        // Save to DB
        val parentFile = file.parentFile
        val seriesId = if (parentFile != null && parentFile.name != "comics") {
            parentFile.name
        } else {
            null
        }

        val comic = ComicEntity(
            id = file.absolutePath,
            title = file.nameWithoutExtension,
            filePath = file.absolutePath,
            coverPath = coverPath ?: "",
            pageCount = pageCount,
            seriesId = seriesId
        )
        dao.insert(comic)
    }

    override suspend fun generateCover(id: String): String? = withContext(Dispatchers.IO) {
        val file = File(id)
        if (!file.exists()) return@withContext null

        val coverDir = File(context.filesDir, "covers").apply { mkdirs() }
        val coverFile = File(coverDir, "${file.nameWithoutExtension}_cover.jpg")

        try {
            when {
                file.isZip() -> {
                    ZipFile(file).use { zip ->
                        val entries = zip.entries().toList().filter { it.isImage() }.sortedPages()
                        if (entries.isNotEmpty()) {
                            zip.getInputStream(entries.first()).use { input ->
                                val bitmap = BitmapFactory.decodeStream(input)
                                saveBitmapAsCover(bitmap, coverFile)
                            }
                        }
                    }
                }
                file.isPdf() -> {
                    val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            saveBitmapAsCover(bitmap, coverFile)
                        }
                    }
                    renderer.close()
                    pfd.close()
                }
                file.isDirectory -> {
                    val images = file.listFiles()?.filter { it.isImageFile() }?.sortedBy { it.name }
                    if (!images.isNullOrEmpty()) {
                        val bitmap = BitmapFactory.decodeFile(images.first().absolutePath)
                        saveBitmapAsCover(bitmap, coverFile)
                    }
                }
            }
            
            if (coverFile.exists()) {
                val path = coverFile.absolutePath
                dao.updateCoverPath(id, path)
                return@withContext path
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun saveBitmapAsCover(bitmap: Bitmap?, coverFile: File) {
        bitmap?.let {
            FileOutputStream(coverFile).use { output ->
                it.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }
        }
    }

    private fun File.isZip() = extension.lowercase() in listOf("cbz", "zip")
    private fun File.isPdf() = extension.lowercase() == "pdf"
    private fun File.isImageFile() = extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")

    override suspend fun updateSeries(id: String, seriesId: String?) {
        dao.updateSeries(id, seriesId)
    }

    override suspend fun updateGenres(id: String, genres: List<dev.nalamzap.comig.domain.model.Genre>) {
        dao.updateGenres(id, genres)
    }

    override fun observeSeriesTitles(): Flow<List<String>> {
        return dao.observeSeriesTitles()
    }

    override fun observeSeries(): Flow<List<Series>> {
        return dao.observeSeries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateSeriesGenres(id: String, genres: List<Genre>) {
        if (dao.getSeriesById(id) == null) {
            dao.insertSeries(SeriesEntity(id = id, title = id))
        }
        dao.updateSeriesGenres(id, genres)
    }

    override suspend fun updateSeriesCover(id: String, coverPath: String?) {
        if (dao.getSeriesById(id) == null) {
            dao.insertSeries(SeriesEntity(id = id, title = id))
        }
        dao.updateSeriesCover(id, coverPath)
    }

    override suspend fun deleteComic(id: String) {
        val comic = dao.getComicById(id) ?: return
        // Delete cover file
        if (comic.coverPath.isNotEmpty()) {
            File(comic.coverPath).delete()
        }
        // Delete the actual comic file from internal storage
        File(comic.filePath).delete()
        dao.deleteComicById(id)
    }

    override suspend fun deleteSeries(id: String, deleteComics: Boolean) {
        if (deleteComics) {
            val comics = dao.getComicsBySeries(id)
            comics.forEach { comic ->
                // Delete cover file
                if (comic.coverPath.isNotEmpty()) {
                    File(comic.coverPath).delete()
                }
                // Delete the actual comic file
                File(comic.filePath).delete()
                dao.deleteComicById(comic.id)
            }
        } else {
            dao.unlinkComicsFromSeries(id)
        }
        dao.deleteSeriesById(id)
    }

    private var cachedZip: ZipFile? = null
    private var cachedUri: Uri? = null

    private fun getZip(uri: Uri): ZipFile {
        if (cachedUri != uri) {
            cachedZip?.close()
            val file = when (uri.scheme) {
                "content" -> {
                    val tempFile = File(context.cacheDir, "temp_reader.cbz")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                else -> File(uri.path ?: "")
            }
            
            if (!file.exists()) {
                throw java.io.FileNotFoundException("File not found: ${file.absolutePath}")
            }
            
            try {
                cachedZip = ZipFile(file)
                cachedUri = uri
            } catch (e: Exception) {
                cachedZip = null
                cachedUri = null
                throw e
            }
        }
        return cachedZip!!
    }

    override suspend fun getPages(uri: Uri): List<ComicPage> =
        withContext(Dispatchers.IO) {
            val file = File(uri.path ?: "")
            when {
                file.isZip() -> {
                    val zip = getZip(uri)
                    zip.entries()
                        .toList()
                        .filter { it.isImage() }
                        .sortedPages()
                        .mapIndexed { index, entry ->
                            ComicPage(index, entry.name)
                        }
                }
                file.isPdf() -> {
                    val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    val count = renderer.pageCount
                    renderer.close()
                    pfd.close()
                    (0 until count).map { ComicPage(it, it.toString()) }
                }
                file.isDirectory -> {
                    file.listFiles()?.filter { it.isImageFile() }?.sortedBy { it.name }?.mapIndexed { index, img ->
                        ComicPage(index, img.absolutePath)
                    } ?: emptyList()
                }
                else -> emptyList()
            }
        }

    override suspend fun loadPageBitmap(
        uri: Uri,
        page: ComicPage,
        targetWidth: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val file = File(uri.path ?: "")
        when {
            file.isZip() -> {
                val zip = getZip(uri)
                val entry = zip.getEntry(page.entryName)

                // ---- 1. Decode bounds
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                zip.getInputStream(entry).use {
                    BitmapFactory.decodeStream(it, null, boundsOptions)
                }

                // ---- 2. Calculate sample size (WIDTH ONLY)
                val sampleSize = calculateInSampleSize(
                    boundsOptions.outWidth,
                    targetWidth
                )

                // ---- 3. Decode actual bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565// faster, lower memory
                }

                zip.getInputStream(entry).use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                        ?: error("Bitmap decode failed for ${page.entryName}")
                }
            }
            file.isPdf() -> {
                android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        renderer.openPage(page.index).use { pdfPage ->
                            val scale = targetWidth.toFloat() / pdfPage.width.toFloat()
                            val height = (pdfPage.height * scale).toInt()
                            val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                            pdfPage.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            }
            file.isDirectory -> {
                val imgFile = File(page.entryName)
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imgFile.absolutePath, boundsOptions)

                val sampleSize = calculateInSampleSize(boundsOptions.outWidth, targetWidth)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(imgFile.absolutePath, decodeOptions)
                    ?: error("Bitmap decode failed for ${page.entryName}")
            }
            else -> error("Unsupported file type")
        }
    }


    private fun calculateInSampleSize(
        width: Int,
        reqWidth: Int
    ): Int {
        var sampleSize = 1
        while (width / sampleSize > reqWidth * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun getDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return "comic.cbz"
    }
}
