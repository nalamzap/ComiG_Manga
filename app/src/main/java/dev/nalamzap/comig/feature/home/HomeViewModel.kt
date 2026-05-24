package dev.nalamzap.comig.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nalamzap.comig.data.repository.ComicRepositoryImpl
import dev.nalamzap.comig.domain.model.Comic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ComicRepositoryImpl(app)

    val recentlyRead: StateFlow<List<Comic>> = repository.observeRecentlyRead()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
