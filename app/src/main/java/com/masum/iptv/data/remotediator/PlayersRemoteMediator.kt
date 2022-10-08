 package com.masum.iptv.data.remotediator

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.masum.iptv.data.db.AppDataBase
import com.masum.iptv.data.fileparser.ParseLocalFile
import com.masum.iptv.models.Channel
import com.masum.iptv.models.DataType
import dev.ronnie.allplayers.data.entity.RemoteKeys
import retrofit2.HttpException
import java.io.IOException


 @ExperimentalPagingApi
class PlayersRemoteMediator(
    private val dataType: DataType,
    val context: Context,
    private val db: AppDataBase
) : RemoteMediator<Int, Channel>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Channel>): MediatorResult {
        val key = when (loadType) {
            LoadType.REFRESH -> {
        //        if (db.channelDao.count() > 0) return MediatorResult.Success(false)
                null
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                getKey()
            }
        }

        try {
            if (key != null) {
                if (key.isEndReached) return MediatorResult.Success(endOfPaginationReached = true)
            }

            val page: Int = key?.nextKey ?: 1

         //   val playersList = ParseLocalFile("", id!!)

            val endOfPaginationReached =true
            db.withTransaction {
                val nextKey = page + 1

                db.remoteKeysDao.insertKey(
                    RemoteKeys(
                        0,
                        nextKey = nextKey,
                        isEndReached = endOfPaginationReached
                    )
                )
             //   db.channelDao.insertMultipleChannels(playersList)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getKey(): RemoteKeys? {
        return db.remoteKeysDao.getKeys().firstOrNull()
    }


}