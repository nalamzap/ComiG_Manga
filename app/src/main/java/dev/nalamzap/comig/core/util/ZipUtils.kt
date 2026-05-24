package dev.nalamzap.comig.core.util

import java.util.zip.ZipEntry

private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

fun ZipEntry.isImage(): Boolean {
    val name = this.name.lowercase()
    return IMAGE_EXTENSIONS.any { name.endsWith(it) }
}

fun List<ZipEntry>.sortedPages(): List<ZipEntry> =
    this.sortedBy { it.name }