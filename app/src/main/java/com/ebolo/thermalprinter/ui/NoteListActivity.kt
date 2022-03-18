package com.ebolo.thermalprinter.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ebolo.thermalprinter.R
import com.ebolo.thermalprinter.models.NoteModel
import com.ebolo.thermalprinter.viewmodel.NoteViewModel
import com.ebolo.thermalprinter.adapter.NoteAdapter
import kotlinx.android.synthetic.main.activity_note_list.*
import java.text.SimpleDateFormat
import java.util.*

class NoteListActivity : AppCompatActivity(), NoteAdapter.NoteClickInterface {

    lateinit var viewModal: NoteViewModel

    lateinit var mBluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        setNoteList()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
            checkBluetoothPermission()
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R){
            initializeBluetooth()
        }

        fb_newNote.setOnClickListener {
            openTitleDialog()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
        }
    }

    private fun checkBluetoothPermission() {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("test006", "${it.key} = ${it.value}")
                }
            }


        var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
            }else{
                //deny
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                //Manifest.permission.BLUETOOTH_ADMIN,
                //Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private fun openTitleDialog() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_toolbar_dialog, null)
        dialogBuilder.setView(dialogView)

        val etToolbar = dialogView.findViewById(R.id.et_toolbar_edit) as TextView

        dialogBuilder.setTitle("Add Note Title")
        //dialogBuilder.setMessage("Enter data below")

        dialogBuilder.setPositiveButton(Html.fromHtml("<font color='#228B22'>Add</font>"), DialogInterface.OnClickListener { _, _ ->
            if(etToolbar.text.toString().isEmpty() ||  etToolbar.text.toString().trim().isEmpty()){
                etToolbar.error = "Write Note Title"
                etToolbar.requestFocus()
                etToolbar.performClick()
            }else {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("isNew", "1")
                intent.putExtra("title", etToolbar.text.toString())
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

        val adapter = NoteAdapter(this, this)

        note_recyclerview.adapter = adapter

        viewModal = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NoteViewModel::class.java]

        viewModal.allNotes.observe(this, Observer { list ->
            list?.let {
                if(it.isNullOrEmpty()){
                    linearLayout.visibility = View.GONE
                    tv_emptyList.visibility = View.VISIBLE
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

    override fun onNoteClick(note: NoteModel) {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_toolbar_dialog, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)

        val etToolbar = dialogView.findViewById(R.id.et_toolbar_edit) as EditText

        dialogBuilder.setTitle("Update Note Title")

        etToolbar.setText(note.note_title)

        dialogBuilder.setPositiveButton(Html.fromHtml("<font color='#228B22'>Update</font>"), DialogInterface.OnClickListener { _, _ ->
            if(etToolbar.text.toString().isEmpty() ||  etToolbar.text.toString().trim().isEmpty()){
                etToolbar.error = "Write Note Title"
                etToolbar.requestFocus()
                etToolbar.performClick()
            }else {
                var sdf = SimpleDateFormat("dd/MMM/yyyy")
                val currentDateAndTime = sdf.format(Date())

                val updatedNote = NoteModel(etToolbar.text.toString(), note.note_desc, currentDateAndTime)
                updatedNote.id = note.id
                viewModal.updateNote(updatedNote)
                Toast.makeText(this, "Note Title Successfully Updated", Toast.LENGTH_SHORT).show()
            }
        })
        dialogBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }
}