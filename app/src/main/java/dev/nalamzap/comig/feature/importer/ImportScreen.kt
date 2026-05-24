package dev.nalamzap.comig.feature.importer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ImportScreen(onDone: () -> Unit) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val input = context.contentResolver.openInputStream(uri)!!
        val comicsDir = File(context.filesDir, "comics").apply { mkdirs() }
        val outFile = File(comicsDir, uri.lastPathSegment ?: "comic.cbz")

        outFile.outputStream().use { output ->
            input.copyTo(output)
        }

        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            launcher.launch(arrayOf("application/*"))
        }) {
            Text("Import CBZ / CBR")
        }
    }
}
