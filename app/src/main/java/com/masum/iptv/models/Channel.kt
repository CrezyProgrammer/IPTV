package com.masum.iptv.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    val id:String,
    val location:String,
    @PrimaryKey(autoGenerate = false)
    val title:String,
    val category:String,
    val logo:String
)
