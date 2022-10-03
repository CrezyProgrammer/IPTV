package com.masum.iptv.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masum.iptv.models.Channel
import java.util.concurrent.Flow

@Dao
interface ChannelDao {
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertMultipleChannels(list: List<Channel>)

@Query("Select category  from channels group by category ")
 fun getCategoryList(): LiveData<List<String>>

@Query("SELECT * FROM channels WHERE category LIKE '%' || :query || '%'")
 fun getAllChannels(query:String): PagingSource<Int, Channel>
 @Query("SELECT COUNT(id) from channels")
 suspend fun count(): Int
}