package com.zinnotech.bluetoothserver.activity

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothserver.R
import com.example.bluetoothserver.databinding.ActivityLowVersionCameraBinding

class LowVersionCameraActivity : AppCompatActivity() {

    private lateinit var bind: ActivityLowVersionCameraBinding
    private lateinit var previewSurface: Surface
    private var imageReader: ImageReader? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLowVersionCameraBinding.inflate(layoutInflater)
        setContentView(bind.surfaceView)

        if(!checkCameraHardware(applicationContext))    finish()

        imageReader = ImageReader.newInstance(bind.surfaceView.width, bind.surfaceView.height, ImageFormat.JPEG, 1)
        previewSurface = bind.surfaceView.holder.surface
        
    }

    private fun checkCameraHardware(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(applicationContext, "This device has camera", Toast.LENGTH_SHORT).show()
            return true
        } else {
            Toast.makeText(applicationContext, "No Camera on this device", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}