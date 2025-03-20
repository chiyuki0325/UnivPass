package ink.chyk.pass

import android.annotation.*
import android.webkit.WebSettings.*
import androidx.activity.compose.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.viewinterop.*
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.*


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomWebView(
  url: String,
  state: MutableState<WebView?> = remember { mutableStateOf(null) },
  modifier: Modifier = Modifier,
  isDarkMode: Boolean = isSystemInDarkTheme(),
  darkModeJs: String = """
                  (() => {
                    const css = document.createElement('style')
                    css.type = 'text/css'
                    css.innerHTML = 'html { filter: invert(1) hue-rotate(180deg) saturate(1); }'
                    document.head.appendChild(css)
                  })()
                  """.trimIndent(),
  listenBack: Boolean = true,
  finish: () -> Unit,
  cookies: String? = null,
  extraScripts: String? = null
) {
  // 封装 TBS WebView 组件
  // 部分代码来自 https://stackoverflow.com/questions/34986413/location-is-accessed-in-chrome-doesnt-work-in-webview/34990055

  val ctx = LocalContext.current

  AndroidView(factory = { context ->
    val webView = WebView(context).apply {
      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        setGeolocationEnabled(true)
        setAppCacheEnabled(true)
        setGeolocationDatabasePath(ctx.filesDir.path)
        mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
        useWideViewPort = true
        allowFileAccess = true
        allowContentAccess = true
        safeBrowsingEnabled = false


        webChromeClient = object : WebChromeClient() {
          override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissionsCallback?
          ) {
            callback?.invoke(origin, true, false)
          }
        }

        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            // 允许页面跳转
            view?.loadUrl(url)
            return true
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            // 注入暗黑模式 css
            super.onPageFinished(view, url)
            if (isDarkMode) {
              view?.evaluateJavascript(
                darkModeJs
              ) {}
            }
            // 注入其他脚本
            if (extraScripts != null) {
              view?.evaluateJavascript(extraScripts) {}
            }
          }
        }
      }
    }

    // 储存 webview 实例
    state.value = webView

    // 注入 cookie
    if (cookies != null) {
      setCookieForDomain(url, cookies)
    }

    webView.loadUrl(url)
    webView
  }, modifier = modifier.fillMaxSize())

  BackHandler {
    if (listenBack && state.value?.canGoBack() == true) {
      state.value?.goBack()
    } else {
      finish()
    }
  }
}


private fun setCookieForDomain(url: String, cookie: String) {
  val cookieManager = CookieManager.getInstance().apply {
    setAcceptCookie(true)
    setCookie(url, cookie)
  }

  cookieManager.flush()
}