package com.kaii.bphotos.models.search_page

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.bphotos.database.MediaDatabase
import com.kaii.bphotos.datastore.SQLiteQuery
import com.kaii.bphotos.helpers.MediaItemSortMode
import com.kaii.bphotos.mediastore.MediaStoreData
import com.kaii.bphotos.mediastore.MultiAlbumDataSource
import com.kaii.bphotos.models.multi_album.DisplayDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
	context: Context,
	sortBy: MediaItemSortMode,
	displayDateFormat: DisplayDateFormat,
	database: MediaDatabase
) : ViewModel() {
	private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
				    MultiAlbumDataSource(
				    	context = context,
				    	queryString = SQLiteQuery(query = "", paths = null, includedBasePaths = null),
				    	sortBy = sortBy,
				    	cancellationSignal = cancellationSignal,
						displayDateFormat = displayDateFormat,
						database = database
				    )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> = mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    fun cancelMediaFlow() = cancellationSignal.cancel()
}
