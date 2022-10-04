package com.masum.iptv.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    var location:String,
    var title:String,
    val isURL:Boolean,
    val lastModified:Long
)
