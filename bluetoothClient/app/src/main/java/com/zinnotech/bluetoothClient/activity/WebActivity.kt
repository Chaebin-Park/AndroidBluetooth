package com.zinnotech.bluetoothClient.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Build.VERSION.SDK
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.example.bluetoothClient.R
import com.example.bluetoothClient.databinding.ActivityWebBinding

class WebActivity : AppCompatActivity() {

    private lateinit var bind: ActivityWebBinding
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityWebBinding.inflate(layoutInflater)
        setContentView(bind.root)

        url = intent.getStringExtra(resources.getString(R.string.intent_data)) as String

        Log.e("URL_TEST_WEB", url)

        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        bind.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                bind.progressCircular.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                bind.progressCircular.visibility = View.GONE
            }
        }
        bind.webView.settings.javaScriptEnabled = true
        bind.webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if(bind.webView.canGoBack()) {
            bind.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}