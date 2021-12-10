package com.zinnotech.bluetoothClient.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothClient.R

import com.example.bluetoothClient.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.zinnotech.bluetoothClient.AppController
import com.zinnotech.bluetoothClient.net.BluetoothClient
import com.zinnotech.bluetoothClient.net.SocketListener
import java.util.*
import android.widget.TextView.OnEditorActionListener

class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var sbLog = StringBuilder()
    private lateinit var btClient: BluetoothClient

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }

        bind.etMessage.requestFocus()

        resultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(applicationContext, "블루투스 활성화", Toast.LENGTH_LONG).show()
                finish()
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(applicationContext, "취소", Toast.LENGTH_LONG).show()
            }
        }

        this.btClient = BluetoothClient(this)
        AppController.Instance.init(this, btClient)
        btClient.setOnSocketListener(mOnSocketListener)

        setListener()

        bind.etMessage.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    if (bind.etMessage.text.toString().isNotEmpty()) {
                        btClient.sendData(bind.etMessage.text.toString())
                        bind.etMessage.setText("")
                    }
                }
                else ->
                    return@OnEditorActionListener false
            }
            true
        })
    }

    private fun setListener() {
        bind.btnScan.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            resultLauncher.launch(intent)
        }

        bind.btnDisconnect.setOnClickListener {
            btClient.disconnectFromServer()
        }

        bind.btnClearData.setOnClickListener {
            sbLog.clear()
            bind.tvLogView.text = ""
        }

        bind.btnCamera.setOnClickListener {
            btClient.sendData("CAMERA")
        }

        bind.btnQr.setOnClickListener {
            btClient.sendData("QR")
        }

    }

    private fun log(message: String) {
        sbLog.append(message.trimIndent() + "\n")
        handler.post {
            bind.tvLogView.text = sbLog.toString()
            bind.svLogView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private val mOnSocketListener: SocketListener = object : SocketListener {
        override fun onConnect() {
            log("Connect!\n")
        }

        override fun onDisconnect() {
            log("Disconnect!\n")
        }

        override fun onError(e: Exception?) {
            e?.let { log(e.toString() + "\n") }
        }

        override fun onReceive(msg: String?) {
            val urlRegex = "(https|http)://(.+)".toRegex()

            if (msg != null) {
                when {
                    msg.length >= 100 -> {
                        Log.e("LOG_TEST", msg)
                        var str = msg.replace("\n", "").replace(" ", "")
                        str = str.trimIndent()
                        val intent = Intent(this@MainActivity, BitmapActivity::class.java)
                        intent.putExtra(resources.getString(R.string.intent_data), str)
                        startActivity(intent)
                    }
                    msg.matches(urlRegex) -> {
                        Log.e("URL_TEST_MAIN", msg)
                        log("Receive : $msg\n")
                        val intent = Intent(this@MainActivity, WebActivity::class.java)
                        intent.putExtra(resources.getString(R.string.intent_data), msg)
                        startActivity(intent)
                    }
                    msg.trimIndent() == "KEYBOARD" -> {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(bind.etMessage, InputMethodManager.SHOW_IMPLICIT)
                    }
                    else -> {
                        log("Receive : $msg\n")
                    }
                }
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