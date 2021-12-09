package com.example.bluetoothserver

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.bluetoothserver.databinding.ActivityCameraBinding
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.util.*
import java.util.jar.Manifest

class CameraActivity : AppCompatActivity() {

    private lateinit var bind: ActivityCameraBinding
    private lateinit var surfaceViewHolder: SurfaceHolder
    private lateinit var imageReader: ImageReader
    private lateinit var cameraDevice: CameraDevice
    private lateinit var previewBuilder: CaptureRequest.Builder
    private lateinit var session: CameraCaptureSession

    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityCameraBinding.inflate(layoutInflater)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(bind.root)

        bind.svCamera.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                initPreview()
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraDevice.close()
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun initPreview() {
        val handlerThread = HandlerThread("CAMERA2")
        handlerThread.start()
        this.handler = Handler(handlerThread.looper)

        openCamera()
    }

    private fun openCamera() {
        try {
            val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics("0")
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val largestPreviewSize = map!!.getOutputSizes(ImageFormat.JPEG)[0]

            imageReader = ImageReader.newInstance(
                largestPreviewSize.width,
                largestPreviewSize.height,
                ImageFormat.JPEG,
                7
            )

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            cameraManager.openCamera("0", deviceStateCallback, handler)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            try {
                takePreview()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onDisconnected(p0: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(p0: CameraDevice, p1: Int) {

        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private fun takePreview() {
        previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewBuilder.addTarget(surfaceViewHolder.surface)
        val sessionConfiguration =
            SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                Collections.singletonList(OutputConfiguration(surfaceViewHolder.surface)),
                HandlerExecutor(handler!!.looper),
                sessionPreviewStateCallback
            )
        cameraDevice.createCaptureSession(sessionConfiguration)
        //cameraDevice.createCaptureSession(
        //    listOf(surfaceViewHolder.surface, imageReader.surface),
        //    sessionPreviewStateCallback,
        //    handler
        //)
    }

    private val sessionPreviewStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(s: CameraCaptureSession) {
            session = s
            try {
                previewBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )

                session.setRepeatingRequest(previewBuilder.build(), null, handler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(p0: CameraCaptureSession) {}
    }
}