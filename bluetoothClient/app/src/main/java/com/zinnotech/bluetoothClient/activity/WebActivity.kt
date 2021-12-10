package com.zinnotech.bluetoothClient.activity

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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

        initWebView()
    }

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

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(url)
                return true
            }
        }

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