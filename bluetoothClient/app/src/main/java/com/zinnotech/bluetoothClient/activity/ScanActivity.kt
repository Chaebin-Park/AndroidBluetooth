package com.zinnotech.bluetoothClient.activity

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zinnotech.bluetoothClient.AppController
import com.zinnotech.bluetoothClient.adapter.DeviceAdapter
import com.example.bluetoothClient.databinding.ActivityScanBinding
import com.zinnotech.bluetoothClient.net.BluetoothClient

class ScanActivity : AppCompatActivity() {

    private lateinit var bind: ActivityScanBinding

    private lateinit var btClient: BluetoothClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityScanBinding.inflate(layoutInflater)
        setContentView(bind.root)

        btClient = AppController.Instance.bluetoothClient

        bind.rvDevices.let {
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(AppController.Instance.mainActivity)
            it.adapter = DeviceAdapter(btClient.getPairedDevices(), onConnectListener)
        }
    }

    private val onConnectListener: OnConnectListener = object : OnConnectListener {
        override fun connectToServer(device: BluetoothDevice) {
            btClient.connectToServer(device)
            finish()
        }
    }

    interface OnConnectListener {
        fun connectToServer(device: BluetoothDevice)
    }

}