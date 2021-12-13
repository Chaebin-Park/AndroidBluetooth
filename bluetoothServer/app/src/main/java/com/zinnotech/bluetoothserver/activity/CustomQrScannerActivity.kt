package com.zinnotech.bluetoothserver.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bluetoothserver.R
import com.example.bluetoothserver.databinding.ActivityCustomQrScannerBinding
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomQrScannerActivity : AppCompatActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var bind: ActivityCustomQrScannerBinding
    private lateinit var cm: CaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityCustomQrScannerBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val bv = findViewById<DecoratedBarcodeView>(R.id.db_qr)

        cm = CaptureManager(this@CustomQrScannerActivity, bv).apply {
            initializeFromIntent(intent, savedInstanceState)
            decode()
        }
    }

    override fun onResume() {
        super.onResume()
        cm.onResume()
    }

    override fun onPause() {
        super.onPause()
        cm.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cm.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cm.onSaveInstanceState(outState)
    }

    override fun onTorchOn() {

    }

    override fun onTorchOff() {

    }
}