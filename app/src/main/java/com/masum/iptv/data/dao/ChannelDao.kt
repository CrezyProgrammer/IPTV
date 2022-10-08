package com.masum.iptv.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masum.iptv.models.Channel

@Dao
interface ChannelDao {
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertMultipleChannels(list: List<Channel>)

@Query("Select category  from channels where playlistId=:id group by category  ")
 fun getCategoryList(id: Int): LiveData<List<String>>

 @Query("Delete  FROM channels where playlistId=:id")
 suspend fun deleteCategoryList(id: Int)

@Query("SELECT * FROM channels WHERE playlistId=:id AND category LIKE '%' || :query || '%'")
 fun getAllChannels(query: String, id: Int): LiveData<List<Channel>>
 @Query("SELECT COUNT(id) from channels")
 suspend fun count(): Int
}