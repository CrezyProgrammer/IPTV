package com.masum.iptv.data.repository

import com.masum.iptv.data.db.AppDataBase
import com.masum.iptv.models.Channel
import com.masum.iptv.models.Playlist
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayersRepository @Inject constructor(
    private val db: AppDataBase,

) {

    fun getAllRecords(query: String, id: Int)= db.channelDao.getAllChannels(query,id )
    fun getAllRecords( id: Int)= db.channelDao.getAllChannels(id )
    fun getAllChannels(id: Int)=db.channelDao.getAllChannels(id)

    suspend fun deletePlaylist(playlist: Playlist) {
        db.channelDao.deleteCategoryList(playlist.id)
        db.playlist.deletePlaylist(playlist)
    }
    suspend fun updatePlaylist(playlist: Playlist)=db.playlist.updatePlaylist(playlist)
     fun  getCategoryList(id: Int) =db.channelDao.getCategoryList(id=id)
    fun getPlaylist()=db.playlist.getPlayList()
    suspend fun insertList(list: List<Channel>)=db.channelDao.insertMultipleChannels(list)
    suspend fun insertPlaylist(playlist: Playlist)=db.playlist.insertPlaylist(playlist)

    /**
     * for caching
     */
    /**
     * Use this if you dont want to cache data to room
     */
//    fun getPlayers(
//    ): Flow<PagingData<Player>> {
//        return Pager(
//            config = PagingConfig(enablePlaceholders = false, pageSize = NETWORK_PAGE_SIZE),
//            pagingSourceFactory = {
//                PlayersDataSource(playersApi)
//            }
//        ).flow
//    }

    /**
     * The same thing but with Livedata
     */


    companion object {
        private const val NETWORK_PAGE_SIZE = 5
    }

}