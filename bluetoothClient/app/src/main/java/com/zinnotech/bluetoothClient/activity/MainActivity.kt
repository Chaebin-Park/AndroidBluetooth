package com.zinnotech.bluetoothClient.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

import com.example.bluetoothClient.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.zinnotech.bluetoothClient.AppController
import com.zinnotech.bluetoothClient.net.BTConstant.BT_REQUEST_ENABLE
import com.zinnotech.bluetoothClient.net.BluetoothClient
import com.zinnotech.bluetoothClient.net.SocketListener
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var sbLog = StringBuilder()
    private lateinit var btClient: BluetoothClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }

        this.btClient = BluetoothClient(this)
        AppController.Instance.init(this, btClient)
        btClient.setOnSocketListener(mOnSocketListener)

        setListener()
    }

    private fun setListener() {
        bind.btnScan.setOnClickListener {
            ScanActivity.startForResult(this, 102)
        }

        bind.btnDisconnect.setOnClickListener {
            btClient.disconnectFromServer()
        }

        bind.btnSendData.setOnClickListener {
            if (bind.etMessage.text.toString().isNotEmpty()) {
                btClient.sendData(bind.etMessage.text.toString())
                bind.etMessage.setText("")
            }
        }

        bind.btnCamera.setOnClickListener {
            btClient.sendData("CAMERA")
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
            msg?.let { log("Receive : $it\n") }
        }

        override fun onSend(msg: String?) {
            msg?.let { log("Send : $it\n") }
        }

        override fun onLogPrint(msg: String?) {
            msg?.let { log("$it\n") }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            BT_REQUEST_ENABLE -> if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(applicationContext, "블루투스 활성화", Toast.LENGTH_LONG).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(applicationContext, "취소", Toast.LENGTH_LONG).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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