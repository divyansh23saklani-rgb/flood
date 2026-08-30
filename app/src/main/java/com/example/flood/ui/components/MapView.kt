package com.example.flood.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

class MapBridge(
    private val onMapClick: (Double, Double) -> Unit,
    private val onDirections: (Double, Double) -> Unit
) {
    @JavascriptInterface
    fun postMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val type = json.optString("type")
            val payload = json.optJSONObject("payload")

            if (type == "mapPress" && payload != null) {
                val lat = payload.optDouble("lat")
                val lng = payload.optDouble("lng")
                onMapClick(lat, lng)
            } else if (type == "directions" && payload != null) {
                val lat = payload.optDouble("lat")
                val lng = payload.optDouble("lng")
                onDirections(lat, lng)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapView(
    mapDataJson: String,
    onMapClick: (Double, Double) -> Unit,
    onDirections: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript("window.updateMapData($mapDataJson);", null)
                }
            }

            addJavascriptInterface(MapBridge(onMapClick, onDirections), "AndroidBridge")
            loadUrl("file:///android_asset/leafletMap.html")
        }
    }

    LaunchedEffect(mapDataJson) {
        webView.evaluateJavascript("if (window.updateMapData) { window.updateMapData($mapDataJson); }", null)
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
