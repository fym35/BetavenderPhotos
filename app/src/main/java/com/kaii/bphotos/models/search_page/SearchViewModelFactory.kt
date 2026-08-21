package com.kaii.bphotos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.bphotos.database.MediaDatabase
import com.kaii.bphotos.helpers.MediaItemSortMode
import com.kaii.bphotos.models.multi_album.DisplayDateFormat

@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory(
	private val context: Context,
	private val sortBy: MediaItemSortMode,
	private val displayDateFormat: DisplayDateFormat,
	private val database: MediaDatabase
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == SearchViewModel::class.java) {
			return SearchViewModel(context, sortBy, displayDateFormat, database) as T
		}
		throw IllegalArgumentException("SearchViewModel: Cannot cast ${modelClass.simpleName} as ${SearchViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
