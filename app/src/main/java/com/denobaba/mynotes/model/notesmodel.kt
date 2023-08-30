package com.denobaba.mynotes.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Notess")
data class NotesModel(

    @PrimaryKey(autoGenerate = true) val uuid: Int?,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "date")
    val date:String?,

    @ColumnInfo(name = "locked")
    val locked: Boolean = false,

    @ColumnInfo(name = "password")
    val password: String? = null,

    @ColumnInfo(name = "alarmStatus")
    var alarmStatus: Boolean = false,

    @ColumnInfo(name = "pinned")
    var pinned: Boolean = false








)