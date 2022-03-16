package com.ebolo.thermalprinter

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ebolo.thermalprinter.viewmodel.NoteViewModel
import com.example.thermalprinter.adapter.NoteAdapter
import kotlinx.android.synthetic.main.activity_note_list.*

class NoteListActivity : AppCompatActivity() {

    lateinit var viewModal: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        setNoteList()

        fb_newNote.setOnClickListener {
            openTitleDialog()
        }
    }

    private fun openTitleDialog() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_toolbar_dialog, null)
        dialogBuilder.setView(dialogView)

        val et_toolbar = dialogView.findViewById(R.id.et_toolbar_edit) as TextView

        dialogBuilder.setTitle("Add Note Title")
        //dialogBuilder.setMessage("Enter data below")

        dialogBuilder.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Add</font>"), DialogInterface.OnClickListener { _, _ ->
            if(et_toolbar.text.toString().isEmpty() ||  et_toolbar.text.toString().trim().isEmpty()){
                et_toolbar.error = "Write Note Title"
                et_toolbar.requestFocus()
                et_toolbar.performClick()
            }else {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("isNew", "1")
                intent.putExtra("title", et_toolbar.text.toString())
                startActivity(intent)
            }
        })
        dialogBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }

    private fun setNoteList() {
        note_recyclerview.layoutManager = LinearLayoutManager(this)

        val adapter = NoteAdapter(this)

        note_recyclerview.adapter = adapter

        viewModal = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NoteViewModel::class.java]

        viewModal.allNotes.observe(this, Observer { list ->
            list?.let {
                if(it.isNullOrEmpty()){
                    linearLayout.visibility = View.GONE
                } else {
                    adapter.setList(it)
                }
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}