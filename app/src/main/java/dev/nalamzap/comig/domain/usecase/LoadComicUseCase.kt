package dev.nalamzap.comig.domain.usecase

import android.net.Uri
import dev.nalamzap.comig.domain.repository.ComicRepository

class LoadComicUseCase(
    private val repository: ComicRepository
) {
    suspend operator fun invoke(uri: Uri) =
        repository.loadComic(uri)
}
