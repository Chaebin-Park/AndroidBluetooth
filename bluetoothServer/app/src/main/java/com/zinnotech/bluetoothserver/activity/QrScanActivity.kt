package com.zinnotech.bluetoothserver.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothserver.R
import com.example.bluetoothserver.databinding.ActivityQrScanBinding
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*

class QrScanActivity : AppCompatActivity() {

    private lateinit var bind: ActivityQrScanBinding

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val intentResult = IntentIntegrator.parseActivityResult(it.resultCode, it.data)

        if(intentResult.contents == null){
            setResult(RESULT_CANCELED)
        } else {
            Toast.makeText(this, "${intentResult.contents}\n${it.data}", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@QrScanActivity, MainActivity::class.java)
            intent.putExtra(resources.getString(R.string.intent_data), intentResult.contents)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val integrator = IntentIntegrator(this).apply {
            captureActivity = CustomQrScannerActivity::class.java
            setTimeout(15000)
            setPrompt("QR 인증")
        }

        resultLauncher.launch(integrator.createScanIntent())
    }
}