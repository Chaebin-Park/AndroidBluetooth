package com.example.bluetoothClient

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.example.bluetoothClient.activity.ScanActivity
import com.example.bluetoothClient.net.BTConstant.BT_REQUEST_ENABLE
import com.example.bluetoothClient.net.BluetoothClient
import com.example.bluetoothClient.net.SocketListener
import java.util.*

class MainActivity : AppCompatActivity() {

    private var handler: Handler = Handler()
    private var sbLog = StringBuilder()
    private var btClient: BluetoothClient = BluetoothClient()

    private lateinit var svLogView: ScrollView
    private lateinit var tvLogView: TextView
    private lateinit var etMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }

        AppController.Instance.init(this, btClient)

        initUI()
        setListener()

        btClient.setOnSocketListener(mOnSocketListener)
    }

    private fun initUI() {
        svLogView = findViewById(R.id.svLogView)
        tvLogView = findViewById(R.id.tvLogView)
        etMessage = findViewById(R.id.etMessage)
    }

    private fun setListener() {
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            ScanActivity.startForResult(this, 102)
        }

        findViewById<Button>(R.id.btnDisconnect).setOnClickListener {
            btClient.disconnectFromServer()
        }

        findViewById<Button>(R.id.btnSendData).setOnClickListener {
            if (etMessage.text.toString().isNotEmpty()) {
                btClient.sendData(etMessage.text.toString())
                //etCommand.setText("")
            }
        }
    }

    private fun log(message: String) {
        sbLog.append(message.trimIndent() + "\n")
        handler.post {
            tvLogView.text = sbLog.toString()
            svLogView.fullScroll(ScrollView.FOCUS_DOWN)
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
                    TedPermission(this)
                        .setPermissionListener(object : PermissionListener {
                            override fun onPermissionGranted() {

                            }

                            override fun onPermissionDenied(deniedPermissions: ArrayList<String?>) {

                            }
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