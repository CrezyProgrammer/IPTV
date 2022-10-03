package com.masum.iptv.data.repository

import android.content.Context
import androidx.paging.*
import com.masum.iptv.data.db.AppDataBase
import com.masum.iptv.data.remotediator.PlayersRemoteMediator
import com.masum.iptv.models.Channel
import com.masum.iptv.models.DataType
import com.masum.iptv.models.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayersRepository @Inject constructor(
    private val db: AppDataBase,

) {

    fun getAllRecords(query:String): PagingSource<Int, Channel> {
        return db.channelDao.getAllChannels(query )
    }
    private val pagingSourceFactory = { db.channelDao.getAllChannels("")}
     fun  getCategoryList()=db.channelDao.getCategoryList()
    fun getPlaylist()=db.playlist.getPlayList()
    suspend fun insertList(list: List<Channel>)=db.channelDao.insertMultipleChannels(list)
    suspend fun insertPlaylist(playlist: Playlist)=db.playlist.insertPlaylist(playlist)

    /**
     * for caching
     */
    @ExperimentalPagingApi
    fun getPlayers(dataType: DataType,  context: Context): Flow<PagingData<Channel>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = PlayersRemoteMediator(
                dataType,
                context,
                db
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow //can also return livedata
    }
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