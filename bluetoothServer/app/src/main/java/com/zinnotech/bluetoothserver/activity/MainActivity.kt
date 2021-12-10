package com.zinnotech.bluetoothserver.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.*
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
import java.util.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener


class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var sbLog = StringBuilder()
    private lateinit var btServer: BluetoothServer

    private lateinit var qrResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>

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
                val uri = result.data?.getParcelableExtra<Uri>(resources.getString(R.string.intent_data))

//                btServer.sendData("Capture Result : $fileName")
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

        bind.etMessage.setOnClickListener{
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
            if(msg?.trimIndent() == "CAMERA") {
                intent = Intent(this@MainActivity, CameraActivity::class.java)
                cameraResultLauncher.launch(intent)
            } else if(msg?.trimIndent() == "QR") {
                intent = Intent(this@MainActivity, QrScanActivity::class.java)
                qrResultLauncher.launch(intent)
            } else {
                msg?.let { log("Receive : $it\n") }
            }
        }

        override fun onSend(msg: String?) {
            msg?.let { log("Send : $it\n") }
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
                if (element == PackageManager.PERMISSION_GRANTED) {
                } else {
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