package com.masum.iptv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.masum.iptv.data.dao.ChannelDao
import com.masum.iptv.data.dao.PlaylistDao
import com.masum.iptv.data.dao.RemoteKeysDao
import com.masum.iptv.models.Channel
import com.masum.iptv.models.Playlist
import dev.ronnie.allplayers.data.entity.RemoteKeys

@Database(
    entities=[Channel::class, RemoteKeys::class,Playlist::class],
    version=2, exportSchema = true
)
abstract class AppDataBase : RoomDatabase() {

    abstract val channelDao: ChannelDao
    abstract val remoteKeysDao: RemoteKeysDao
    abstract val playlist: PlaylistDao


    companion object {
        @Volatile
        private var instance: AppDataBase? =null
        private val LOCK=Any()

        operator fun invoke(context: Context)=instance
            ?: synchronized(LOCK) {
                instance?:buildDatabase(
                    context
                ).also{
                    instance=it
                }
            }

        private fun buildDatabase(context: Context)=
            Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                "app_db"
            ).fallbackToDestructiveMigration()
                .build()
    }
}