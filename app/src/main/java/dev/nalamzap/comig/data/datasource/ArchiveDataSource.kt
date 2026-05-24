package dev.nalamzap.comig.data.datasource

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipFile

class ArchiveDataSource(private val context: Context) {

    fun openZip(uri: Uri): ZipFile {
        val tempFile = File(context.cacheDir, "archive_${System.currentTimeMillis()}.zip")

        context.contentResolver.openInputStream(uri)!!.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return ZipFile(tempFile)
    }
}