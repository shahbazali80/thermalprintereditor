package com.ebolo.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.ebolo.thermalprinter.async.AsyncBluetoothEscPosPrint
import com.ebolo.thermalprinter.async.AsyncEscPosPrint
import com.ebolo.thermalprinter.async.AsyncEscPosPrinter
import com.ebolo.thermalprinter.models.NoteModel
import com.ebolo.thermalprinter.viewmodel.NoteViewModel
import com.ebolo.thermalprintereditor.fragments.KRichEditorFragment
import com.ebolo.thermalprintereditor.fragments.kRichEditorFragment
import com.ebolo.thermalprintereditor.ui.widgets.EditorButton
import com.ebolo.thermalprintereditor.ui.widgets.EditorButton.Companion.IMAGE
import com.esafirm.imagepicker.features.ImagePicker
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import org.jetbrains.anko.setContentView
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var selectedDevice: BluetoothConnection? = null

    lateinit var mBluetoothAdapter: BluetoothAdapter

    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 9999

    @Volatile
    var isPrinterConnect = false

    var isPermissionAllow = true

    private lateinit var viewModal: NoteViewModel
    var noteID = -1

    var noteTitle: String? = null
    var noteDescription: String? = null
    private var noteDate: String? = null
    private var toolbarTitle: String? = null
    var isNew: String? = null

    private lateinit var editorFragment: KRichEditorFragment
    var currentDateAndTime: String? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivityLayout().setContentView(this)

        var sdf = SimpleDateFormat("dd/MMM/yyyy")
        currentDateAndTime = sdf.format(Date())

        isNew = intent.getStringExtra("isNew")
        noteTitle = intent.getStringExtra("title")
        if (noteTitle != null)
            supportActionBar!!.title = noteTitle

        getValuesForUpdateDelete()

        initializeBluetoothOrRequestPermission()
        //initializeBluetooth()

        openDexter()

        viewModal = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NoteViewModel::class.java]

        editorFragment = supportFragmentManager.findFragmentByTag("EDITOR") as KRichEditorFragment?
            ?: kRichEditorFragment {
                imageButtonAction = { ImagePicker.create(this@MainActivity).start() }
                placeHolder = "Write here..."

                buttonsLayout = listOf(
                    //EditorButton.UNDO,
                    //EditorButton.REDO,
                    //EditorButton.IMAGE,
                    //EditorButton.LINK,
                    EditorButton.BOLD,
                    //EditorButton.ITALIC,
                    EditorButton.UNDERLINE,
                    //EditorButton.SUBSCRIPT,
                    //EditorButton.SUPERSCRIPT,
                    //EditorButton.STRIKETHROUGH,
                    EditorButton.JUSTIFY_LEFT,
                    EditorButton.JUSTIFY_CENTER,
                    EditorButton.JUSTIFY_RIGHT,
                    EditorButton.JUSTIFY_FULL,
                    //EditorButton.ORDERED,
                    //EditorButton.UNORDERED,
                    //EditorButton.CHECK,
                    //EditorButton.NORMAL,
                    //EditorButton.H1,
                    //EditorButton.H2,
                    //EditorButton.H3,
                    //EditorButton.H4,
                    //EditorButton.H5,
                    //EditorButton.H6,
                    //EditorButton.INDENT,
                    //EditorButton.OUTDENT,
                    //EditorButton.BLOCK_QUOTE,
                    //EditorButton.BLOCK_CODE,
                    //EditorButton.CODE_VIEW
                )
                onInitialized = {
                    // Simulate loading saved contents action
                    editorFragment.editor.setContents(
                        noteDescription.toString()
                    )
                }
            }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_holder, editorFragment, "EDITOR")
            .commit()
    }

    private fun openDexter() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.BLUETOOTH)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) { /* ... */
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) { /* ... */
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) { /* ... */
                }
            }).check()
    }

    private fun getValuesForUpdateDelete() {
        if (isNew.equals("0")) {
            noteTitle = intent.getStringExtra("noteTitle")
            noteDescription = intent.getStringExtra("noteDescription")
            noteDate = intent.getStringExtra("noteDate")
            noteID = intent.getIntExtra("noteId", -1)
            supportActionBar!!.title = noteTitle
            toolbarTitle = noteTitle
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main_printer, menu)
        if (isNew.equals("1")) {
            menu.findItem(R.id.action_delete).isVisible = false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_printerOut -> {
                editorFragment.editor.getHtmlContent { html ->
                    if (isPrinterConnect) {
                        printBluetooth(html)
                    } else {
                        Toast.makeText(this, "No Printer Device Connected", Toast.LENGTH_SHORT)
                            .show()
                        val printStr = html.replace("<p class=\"ql-align-center\">", "            ")
                            .replace("<p class=\"ql-align-right\">", " [R]")
                            .replace("class=\"ql-size-large\"", "size='big'")
                            .replace("class=\"ql-size-huge\"", "size='tall'")
                            .replace("strong", "b")
                            .replace("<p>", "")
                            .replace("span", "font")
                            .replace("</p>", "\n")
                            .replace("<br>", "\n")
                        Log.d("TAG", printStr + "\n\n" + html)
                    }
                }
                true
            }
            R.id.action_findPrinter -> {
                if (!isPermissionAllow) {
                    val snack = Snackbar.make(
                        window.decorView,
                        "Bluetooth permissions is not granted",
                        Snackbar.LENGTH_LONG
                    )
                    snack.setAction("Allow", View.OnClickListener {
                        initializeBluetoothOrRequestPermission()
                    })
                    snack.show()
                } else if (!isPrinterConnect && isPermissionAllow) {
                    browseBluetoothDevice()
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth Printer Already Connected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
            R.id.action_save_content -> {
                editorFragment.editor.getContents { contents -> // String\
                    addUpdateNote(contents)
                }
                true
            }
            R.id.action_delete -> {
                deleteNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addUpdateNote(contents: String) {
        var statusMsg: String? = null
        if (isNew.equals("0")) {
            val updatedNote = NoteModel(
                noteTitle.toString(),
                contents,
                currentDateAndTime.toString()
            )
            updatedNote.id = noteID
            viewModal.updateNote(updatedNote)
            statusMsg = "Successfully Updated"
        } else if (isNew.equals("1")) {
            viewModal.addNote(
                NoteModel(
                    noteTitle.toString(),
                    contents,
                    currentDateAndTime.toString()
                )
            )
            statusMsg = "Successfully Added"
        }
        Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, NoteListActivity::class.java))
        finish()
    }

    private fun deleteNote() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete it?")

        val updatedNote =
            NoteModel(toolbarTitle.toString(), noteDescription.toString(), noteDate.toString())
        updatedNote.id = noteID

        builder.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Yes</font>")) { dialog, which ->
            viewModal.deleteNote(updatedNote)
            Toast.makeText(this, "Note Deleted", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, NoteListActivity::class.java))
            finish()
        }

        builder.setNegativeButton("No") { dialog, which ->
            builder.setCancelable(true)
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            val image = ImagePicker.getFirstImageOrNull(data)
            if (image != null) {
                // The second param (true/false) would not reflect BASE64 mode or not
                // Normal URL mode would pass the URL
                /*editorFragment.editor.command(IMAGE, false, "https://" +
                        "beebom-redkapmedia.netdna-ssl.com/wp-content/uploads/2016/01/" +
                        "Reverse-Image-Search-Engines-Apps-And-Its-Uses-2016.jpg")*/

                // For BASE64, image file path would be passed instead
                editorFragment.editor.command(IMAGE, true, image.path)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeBluetoothOrRequestPermission() {
        val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        }

        val missingPermissions = requiredPermissions.filter { permission ->
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            initializeBluetooth()
        } else {
            requestPermissions(missingPermissions.toTypedArray(), BLUETOOTH_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.none { it != PackageManager.PERMISSION_GRANTED }) {
                    // all permissions are granted
                    initializeBluetooth()
                } else {
                    // some permissions are not granted
                    Toast.makeText(this, "permissions are not granted", Toast.LENGTH_SHORT).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    override fun onPause() {
        // Simulate saving action
        editorFragment.editor.getContents { content ->
            Paper.book("demo").write("content", content)
        }
        super.onPause()
    }

    @SuppressLint("MissingPermission")
    fun browseBluetoothDevice() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list
        if (bluetoothDevicesList != null) {
            val items = arrayOfNulls<String>(bluetoothDevicesList.size + 1)
            items[0] = "Default printer"
            var i = 0
            for (device in bluetoothDevicesList) {
                items[++i] = device.device.name
            }
            val alertDialog = android.app.AlertDialog.Builder(this@MainActivity)
            alertDialog.setTitle("Bluetooth printer selection")

            Toast.makeText(
                this,
                "If your Print device not found, click Find Printer",
                Toast.LENGTH_LONG
            ).show()

            alertDialog.setItems(
                items
            ) { dialogInterface, i ->
                val index = i - 1
                if (index == -1) {
                    selectedDevice = null
                } else {
                    selectedDevice = bluetoothDevicesList[index]
                    isPrinterConnect = true
                    Toast.makeText(this, "Bluetooth Printer is connected", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            alertDialog.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Find Printer</font>")) { dialog, which ->
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            }

            val alert = alertDialog.create()
            alert.setCanceledOnTouchOutside(true)
            alert.show()
        }
    }

    private fun printBluetooth(html: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        }
        else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        )
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) else {
            AsyncBluetoothEscPosPrint(
                this,
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(
                        asyncEscPosPrinter: AsyncEscPosPrinter?,
                        codeException: Int
                    ) {
                        Log.e(
                            "Async.OnPrintFinished",
                            "AsyncEscPosPrint.OnPrintFinished : An error occurred !"
                        )
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter?) {
                        Log.i(
                            "Async.OnPrintFinished",
                            "AsyncEscPosPrint.OnPrintFinished : Print is finished !"
                        )
                    }
                }
            )
                .execute(this.getAsyncEscPosPrinter(selectedDevice, html))
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getAsyncEscPosPrinter(
        printerConnection: DeviceConnection?,
        html: String
    ): AsyncEscPosPrinter? {

        val printStr = html.replace("<p class=\"ql-align-center\">", "         ")
            .replace("<p class=\"ql-align-right\">", " [R]")
            .replace("class=\"ql-size-large\"", "size='tall'")
            .replace("class=\"ql-size-huge\"", "size='big'")
            .replace("strong", "b")
            .replace("<p>", "[L]")
            .replace("span", "font")
            .replace("</p>", "\n[L]")
            .replace("<br>", "\n[L]")

        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
        return printer.addTextToPrint(
            "$printStr"
        )
    }

    /*override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete it?")

        builder.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Yes</font>")) { dialog, which ->

        }

        builder.setNegativeButton("No") { dialog, which ->
            builder.setCancelable(true)
        }
        builder.show()
    }*/
}
