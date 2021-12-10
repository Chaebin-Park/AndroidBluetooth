package com.zinnotech.bluetoothClient.activity

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bluetoothClient.R
import com.example.bluetoothClient.databinding.ActivityBitmapBinding
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Base64.DEFAULT
import android.util.Base64.decode
import java.lang.Exception


class BitmapActivity : AppCompatActivity() {

    private lateinit var bind: ActivityBitmapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityBitmapBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val intentData = intent.getStringExtra(resources.getString(R.string.intent_data)) as String
        val bitmap = stringToBitmap(intentData)

        if (bitmap != null) bind.imageView.setImageBitmap(bitmap)
        else    bind.imageView.setBackgroundColor(Color.BLUE)
    }

    private fun stringToBitmap(encodedString: String): Bitmap? {
        return try {
            val encodeByte: ByteArray = decode(encodedString, DEFAULT)
            BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}