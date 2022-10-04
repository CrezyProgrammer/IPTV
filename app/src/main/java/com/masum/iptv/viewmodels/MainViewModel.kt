package com.masum.iptv.viewmodels


import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.masum.iptv.data.repository.PlayersRepository
import com.masum.iptv.models.Channel
import com.masum.iptv.models.DataType
import com.masum.iptv.models.FileType
import com.masum.iptv.models.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PlayersRepository
) : ViewModel() {
    private var currentResult: Flow<PagingData<Channel>>? = null

    @ExperimentalPagingApi
    fun searchPlayers(  context: Context): Flow<PagingData<Channel>> {
        val newResult: Flow<PagingData<Channel>> =
            repository.getPlayers(DataType("",FileType.FILE),  context).cachedIn(viewModelScope)
        currentResult = newResult
        return newResult
    }

    /**
     * Same thing but with Livedata
     */
    fun getAllRecords(query:String): Flow<PagingData<Channel>> {
        return Pager(config = PagingConfig(pageSize = 10, maxSize = 200),
            pagingSourceFactory = {repository.getAllRecords(query)}).flow.cachedIn(viewModelScope)
    }
    fun getPlaylist()=repository.getPlaylist()

    fun getCategoryList()=repository.getCategoryList()
    suspend fun deletePlaylist(playlist: Playlist)=repository.deletePlaylist(playlist)
    suspend fun insertPlaylist(playlist: Playlist)=repository.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: Playlist)=repository.updatePlaylist(playlist)
    suspend fun insertList(list: List<Channel>) {
repository.insertList(list)
    }

}