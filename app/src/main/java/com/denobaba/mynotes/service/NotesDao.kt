package com.denobaba.mynotes.service

import androidx.lifecycle.LiveData
import androidx.room.*
import com.denobaba.mynotes.model.NotesModel


@Dao
interface NotesDao {
    @Insert
    suspend fun insertNote(note: NotesModel): Long


    @Update
    suspend fun update(note: NotesModel)

    // Notları tarihine göre tersten sıralama
    @Query("SELECT * FROM Notess ORDER BY pinned DESC, date DESC")
    fun getAllNotes(): LiveData<List<NotesModel>>

    @Query("SELECT * FROM Notess WHERE uuid = :noteId")
    suspend fun getNotes(noteId: Int): NotesModel

    @Query("DELETE FROM Notess")
    suspend fun deleteAllNotes(): Int

    @Delete
    suspend fun delete(note: NotesModel)

    @Query("UPDATE Notess SET alarmStatus = :status WHERE uuid = :noteId")
    suspend fun updateAlarmStatus(noteId: Int, status: Boolean)

    @Query("SELECT alarmStatus FROM Notess WHERE uuid = :noteId")
    suspend fun getAlarmStatus(noteId: Int): Boolean?



}