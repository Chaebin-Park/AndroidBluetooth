package com.zinnotech.bluetoothserver.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothserver.R
import com.example.bluetoothserver.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.zinnotech.bluetoothserver.AppController
import com.zinnotech.bluetoothserver.net.BluetoothServer
import com.zinnotech.bluetoothserver.net.SocketListener
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var sbLog = StringBuilder()
    private lateinit var btServer: BluetoothServer

    private lateinit var qrResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)



        checkPermission()

        cameraResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(applicationContext, "촬영 성공", Toast.LENGTH_LONG).show()
                val uri: Uri =
                    result.data!!.getParcelableExtra(resources.getString(R.string.intent_data))!!

                try {
                    val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }

                    bind.ivBitmap.setImageBitmap(bitmap)

                    CoroutineScope(Dispatchers.Default).launch {
                        var str =
                            withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                                bitmapToString(bitmap)
                            }
                        delay(1000)
                        str = "START" + str + "END"

                        btServer.sendData(str)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(applicationContext, "취소", Toast.LENGTH_LONG).show()
            }
        }

        qrResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data?.getStringExtra(resources.getString(R.string.intent_data))
                Toast.makeText(applicationContext, data, Toast.LENGTH_SHORT).show()
                btServer.sendData(data.toString())
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(applicationContext, "취소", Toast.LENGTH_LONG).show()
            }
        }

        btServer = BluetoothServer(this)
        AppController.Instance.init(this, btServer)

        setListener()

        bind.etMessage.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    if (bind.etMessage.text.toString().isNotEmpty()) {
                        btServer.sendData(bind.etMessage.text.toString())
                        bind.etMessage.setText("")
                    }
                }
                else ->
                    return@OnEditorActionListener false
            }
            true
        })

        btServer.setOnSocketListener(mOnSocketListener)
        btServer.accept()
    }

    private suspend fun bitmapToString(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos)
        val bytes = baos.toByteArray()
        var str = encodeToString(bytes, DEFAULT).trimIndent()

        str = str.replace("\n", "")
            .replace("\t","")
            .replace(" ", "")

        Log.e("LOG_TEST_MAIN", "LOG_AT: $str")
        Log.e("LOG_TEST_MAIN", "::\n\nLength:   ${str.length}\nBAOS:    ${baos.size()}\nBitmap: ${bitmap.byteCount}")


        return str
    }

    private fun setListener() {
        bind.btnAccept.setOnClickListener {
            btServer.accept()
        }

        bind.btnStop.setOnClickListener {
            btServer.stop()
        }

        bind.btnClearData.setOnClickListener {
            sbLog.clear()
            bind.tvLogView.text = ""
        }

        bind.btnQr.setOnClickListener {
            intent = Intent(this@MainActivity, QrScanActivity::class.java)
            qrResultLauncher.launch(intent)
        }

        bind.etMessage.setOnClickListener {
            btServer.sendData("KEYBOARD")
        }
    }

    fun log(message: String) {
        sbLog.append(message.trimIndent() + "\n")
        handler.post {
            bind.tvLogView.text = sbLog.toString()
            bind.svLogView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private val mOnSocketListener: SocketListener = object : SocketListener {
        override fun onConnect() {
            log("Connect!\n")
            bind.etMessage.isClickable = true
        }

        override fun onDisconnect() {
            log("Disconnect!\n")
            bind.etMessage.isClickable = false
        }

        override fun onError(e: Exception?) {
            e?.let { log(e.toString() + "\n") }
        }

        override fun onReceive(msg: String?) {
            if (msg?.trimIndent() == "CAMERA") {
                intent = Intent(this@MainActivity, CameraActivity::class.java)
                cameraResultLauncher.launch(intent)
            } else if (msg?.trimIndent() == "QR") {
                intent = Intent(this@MainActivity, QrScanActivity::class.java)
                qrResultLauncher.launch(intent)
            } else {
                msg?.let { log("Receive : $it\n") }
            }
        }

        override fun onSend(msg: String?) {
            if (msg!!.length <= 100)
                log("Send : $msg\n")
        }

        override fun onLogPrint(msg: String?) {
            msg?.let { log("$it\n") }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermission() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )

        for (permission in permissions) {
            val chk = checkCallingOrSelfPermission(Manifest.permission.WRITE_CONTACTS)
            if (chk == PackageManager.PERMISSION_DENIED) {
                requestPermissions(permissions, 0)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            for (element in grantResults) {
                if (element != PackageManager.PERMISSION_GRANTED) {
                    TedPermission.with(this)
                        .setPermissionListener(object : PermissionListener {
                            override fun onPermissionGranted() {}
                            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {}
                        })
                        .setDeniedMessage("You have permission to set up.")
                        .setPermissions(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN
                        )
                        .setGotoSettingButton(true)
                        .check();
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        AppController.Instance.bluetoothOff()
    }
}