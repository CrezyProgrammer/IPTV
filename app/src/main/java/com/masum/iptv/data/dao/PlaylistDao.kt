package com.masum.iptv.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masum.iptv.models.Channel
import com.masum.iptv.models.Playlist
import java.util.concurrent.Flow

@Dao
interface PlaylistDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insertPlaylist(playlist: Playlist)

 @Query("Select * from playlist  ")
 fun getPlayList(): LiveData<List<Playlist>>
}
