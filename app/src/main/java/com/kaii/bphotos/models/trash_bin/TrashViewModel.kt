package com.kaii.bphotos.models.trash_bin

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.bphotos.database.MediaDatabase
import com.kaii.bphotos.helpers.MediaItemSortMode
import com.kaii.bphotos.mediastore.MediaStoreData
import com.kaii.bphotos.mediastore.TrashStoreDataSource
import com.kaii.bphotos.models.multi_album.DisplayDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class TrashViewModel(
    context: Context,
    displayDateFormat: DisplayDateFormat,
    appDatabase: MediaDatabase
) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        TrashStoreDataSource(
            context = context,
            sortBy = MediaItemSortMode.LastModified,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat,
            database = appDatabase
        )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
        return mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaSource() {
        cancellationSignal.cancel()
    }
}
