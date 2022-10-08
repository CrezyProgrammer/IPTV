package com.masum.iptv.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import com.masum.iptv.models.Channel
import com.masum.iptv.models.Playlist
import java.util.concurrent.Flow

@Dao
interface PlaylistDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertPlaylist(playlist: Playlist)

 @Query("Select * from playlist  order by lastModified desc")
 fun getPlayList(): LiveData<List<Playlist>>

 @Delete
 suspend fun deletePlaylist(playlist: Playlist)


 @Update
 suspend fun updatePlaylist(playlist: Playlist)

}
