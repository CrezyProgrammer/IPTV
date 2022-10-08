package com.masum.iptv.viewmodels


import androidx.lifecycle.ViewModel
import androidx.paging.*
import com.masum.iptv.data.repository.PlayersRepository
import com.masum.iptv.models.Channel
import com.masum.iptv.models.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PlayersRepository
) : ViewModel() {
    private var currentResult: Flow<PagingData<Channel>>? = null



    /**
     * Same thing but with Livedata
     */

    fun getPlaylist()=repository.getPlaylist()
    fun getAllChannels(query: String, id: Int)=repository.getAllRecords(query,id)
    fun getCategoryList(id:Int)=repository.getCategoryList(id)
    suspend fun deletePlaylist(playlist: Playlist)=repository.deletePlaylist(playlist)
    suspend fun insertPlaylist(playlist: Playlist)=repository.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: Playlist)=repository.updatePlaylist(playlist)
    suspend fun insertList(list: List<Channel>) {
repository.insertList(list)
    }

}