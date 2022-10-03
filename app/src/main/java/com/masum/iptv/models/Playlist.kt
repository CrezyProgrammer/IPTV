package com.masum.iptv.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    val location:String,
    val title:String,
    val isURL:Boolean,
)
