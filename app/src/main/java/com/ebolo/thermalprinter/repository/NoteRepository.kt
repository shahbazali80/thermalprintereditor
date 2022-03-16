package com.ebolo.thermalprinter.repository

import androidx.lifecycle.LiveData
import com.ebolo.thermalprinter.database.NoteDao
import com.ebolo.thermalprinter.models.NoteModel

class NoteRepository (private val noteDao: NoteDao) {

    val allNotes: LiveData<List<NoteModel>> = noteDao.allNotes()

    fun insert(noteModel: NoteModel) {
        noteDao.insertNote(noteModel)
    }

    fun delete(noteModel: NoteModel){
        noteDao.deleteNote(noteModel)
    }

    fun update(noteModel: NoteModel){
        noteDao.updateNote(noteModel)
    }
}