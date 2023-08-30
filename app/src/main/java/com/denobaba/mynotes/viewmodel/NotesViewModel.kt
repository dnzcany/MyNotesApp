package com.denobaba.mynotes.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.denobaba.mynotes.adapter.NotesAdapter
import com.denobaba.mynotes.model.NotesModel
import com.denobaba.mynotes.service.NotesDatabase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val notesDao = NotesDatabase(getApplication()).notesDao()

    // Use LiveData for automatic UI updates
    val allNotes: LiveData<List<NotesModel>> = notesDao.getAllNotes()

    suspend fun get(noteId: Int) = notesDao.getNotes(noteId)

    fun updateNote(note: NotesModel) = viewModelScope.launch {
        val noteInDb = notesDao.getNotes(note.uuid ?: -1)

        val updatedNote = if (noteInDb != null && (noteInDb.title != note.title || noteInDb.notes != note.notes)) {
            note.copy(
                date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()), // yeni tarih
                pinned = noteInDb.pinned // not güncellendiğinde pinned değeri korunur
            )
        } else {
            note.copy(
                pinned = noteInDb?.pinned ?: false // not güncellendiğinde pinned değeri korunur
            )
        }

        notesDao.update(updatedNote)
    }

    fun insertNote(note: NotesModel) = viewModelScope.launch {
        try {
            val noteInDb = notesDao.getNotes(note.uuid ?: -1)
            note.pinned = noteInDb?.pinned ?: false // not eklendiğinde pinned değeri korunur
            notesDao.insertNote(note)
        } catch (e: Exception) {
            Log.e("NotesViewModel", "Failed to insert note: ", e)
        }
    }



    fun deleteNote(note: NotesModel) = viewModelScope.launch {
        notesDao.delete(note)
    }


    fun encrypt(strToEncrypt: String, secret: String) : String? {
        try {
            val iv = ByteArray(16)
            val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)))
            } else {
                TODO("VERSION.SDK_INT < O")
            }
        } catch (e: Exception) {
            println("Error while encrypting: $e")
        }
        return null
    }



    fun encryptAndSaveNote(noteId: Int, password: String) = viewModelScope.launch {
        try {
            val note = notesDao.getNotes(noteId)
            if (note != null) {
                val encryptedNote = note.copy(
                    notes = note.notes?.let { encrypt(it, password) },
                    locked = true,  // This should stay true if we're encrypting
                    password = password
                )
                notesDao.update(encryptedNote)
            }
        } catch (e: Exception) {
            Log.e("Encryption Error", "Failed to encrypt and save note: ", e)
        }
    }

    fun getNotePassword(noteId: Int): Deferred<String?> = viewModelScope.async {
        val note = notesDao.getNotes(noteId)
        note?.password
    }

    fun getNoteLockState(noteId: Int): Deferred<Boolean> = viewModelScope.async {
        val note = notesDao.getNotes(noteId)
        note?.locked ?: false
    }

    fun updateLockedState(id: Int, locked: Boolean) = viewModelScope.launch {
        val noteInDb = notesDao.getNotes(id)
        val updatedNote = noteInDb?.copy(locked = locked)
        if (updatedNote != null) {
            notesDao.update(updatedNote)
        }
    }
    val alarmStatus: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply { value = false }

    fun updateAlarmStatus(noteId: Int, status: Boolean) = viewModelScope.launch {
        notesDao.updateAlarmStatus(noteId, status)
    }

    fun pinNote(noteId: Int) = viewModelScope.launch {
        val note = notesDao.getNotes(noteId)
        val updatedNote = note?.copy(pinned = true)
        if (updatedNote != null) {
            notesDao.update(updatedNote)
        }
    }

    fun unpinNote(noteId: Int) = viewModelScope.launch {
        val note = notesDao.getNotes(noteId)
        val updatedNote = note?.copy(pinned = false)
        if (updatedNote != null) {
            notesDao.update(updatedNote)
        }
    }

    fun getNoteAlarmStatus(noteId: Int): Deferred<Boolean?> {
        return viewModelScope.async {
            notesDao.getAlarmStatus(noteId)
        }
    }


}





