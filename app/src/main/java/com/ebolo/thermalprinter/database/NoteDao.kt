package com.ebolo.thermalprinter.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ebolo.thermalprinter.models.NoteModel

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(noteModel: NoteModel?)

    @Delete
    fun deleteNote(noteModel: NoteModel?)

    @Update
    fun updateNote(model: NoteModel?)

    @Query(" SELECT * FROM tbl_notes")
    fun allNotes(): LiveData<List<NoteModel>>
}