package com.example.flood.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            // Set browser user agent to avoid tile servers blocking generic/empty WebView User Agents
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 FloodAlert/1.0"

            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    evaluateSafeMapData(view, mapDataJson)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                }
            }

            addJavascriptInterface(MapBridge(onMapClick, onDirections), "AndroidBridge")
            loadUrl("file:///android_asset/leafletMap.html")
        }
    }

    LaunchedEffect(mapDataJson) {
        evaluateSafeMapData(webView, mapDataJson)
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

private fun evaluateSafeMapData(webView: WebView?, jsonString: String) {
    if (webView == null) return
    try {
        val base64Json = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val script = """
            (function() {
                try {
                    var decoded = decodeURIComponent(escape(window.atob('$base64Json')));
                    if (window.updateMapData) {
                        window.updateMapData(decoded);
                    }
                } catch(e) {
                    console.error("Failed to decode and update map data:", e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
