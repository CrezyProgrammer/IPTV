package com.masum.iptv.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.masum.iptv.data.repository.PlayersRepository
import com.masum.iptv.models.Channel
import com.masum.iptv.models.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PlayersRepository
) : ViewModel() {

    var books by mutableStateOf(emptyList<Channel>())




    /**
     * Same thing but with Livedata
     */

    fun getPlaylist()=repository.getPlaylist()
     fun getAllChannels(query: String, id: Int) {

        if(query=="All") {
            viewModelScope.launch{
                repository.getAllRecords(id).collect{
                    books=it
                }}
        }
        else {
            viewModelScope.launch{
                repository.getAllRecords(query,id).collect{
                    books=it
                }}
        }
    }
    fun getCategoryList(id:Int)=repository.getCategoryList(id)
    suspend fun deletePlaylist(playlist: Playlist)=repository.deletePlaylist(playlist)
    suspend fun insertPlaylist(playlist: Playlist)=repository.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: Playlist)=repository.updatePlaylist(playlist)
    suspend fun insertList(list: List<Channel>) {
repository.insertList(list)
    }

}