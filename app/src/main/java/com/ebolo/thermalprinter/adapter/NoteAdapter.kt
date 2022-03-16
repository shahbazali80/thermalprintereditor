package com.example.thermalprinter.adapter

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.ebolo.thermalprinter.MainActivity
import com.ebolo.thermalprinter.NoteListActivity
import com.ebolo.thermalprinter.R
import com.ebolo.thermalprinter.models.NoteModel
import com.ebolo.thermalprinter.viewmodel.NoteViewModel

class NoteAdapter(val context: Context) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

    private var allNotes = ArrayList<NoteModel>()

    private lateinit var viewModal: NoteViewModel

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val noteTV: TextView = itemView.findViewById(R.id.idTVNote)
        val dateTV: TextView = itemView.findViewById(R.id.idTVDate)
        val idImgEdit: ImageView = itemView.findViewById(R.id.idImgEdit)
        val idImgDel: ImageView = itemView.findViewById(R.id.idImgDel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.note_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        viewModal = ViewModelProvider( context as NoteListActivity)[NoteViewModel::class.java]

        val id= allNotes[position].id
        val title= allNotes[position].note_title
        val desc= allNotes[position].note_desc
        val noteDate= allNotes[position].timeStamp

        holder.noteTV.text = title
        holder.dateTV.text = noteDate

        holder.idImgEdit.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("isNew", "0")
            intent.putExtra("noteTitle", title)
            intent.putExtra("noteDescription", desc)
            intent.putExtra("noteDate", noteDate)
            intent.putExtra("noteId", id)
            context.startActivity(intent)
        }

        holder.idImgDel.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Alert")
            builder.setMessage("Are you sure you want to delete it?")

            val updatedNote = NoteModel(title, desc, noteDate)
            updatedNote.id = id

            builder.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Yes</font>")) { dialog, which ->
                viewModal.deleteNote(updatedNote)
                Toast.makeText(context, "Note Deleted", Toast.LENGTH_LONG).show()
            }

            builder.setNegativeButton("No") { dialog, which ->
                builder.setCancelable(true)
            }
            builder.show()
        }
    }

    override fun getItemCount(): Int {
        return allNotes.size
    }

    fun setList(newList: List<NoteModel>) {
        allNotes= newList as ArrayList<NoteModel>
        notifyDataSetChanged()
    }
}