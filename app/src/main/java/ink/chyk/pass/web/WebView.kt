package ink.chyk.pass.web

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
import android.util.Log

private const val DARK_MODE_CSS = "html { filter: invert(1) hue-rotate(180deg) saturate(1);"

private val DARK_MODE_JS = """
  (() => {
    const css = document.createElement('style')
    css.type = 'text/css'
    css.innerHTML = '$DARK_MODE_CSS'
    document.head.appendChild(css)
  })()
""".trimIndent()

private val CLEAR_DARK_MODE_JS = """
  (() => {
    for (const el of document.querySelectorAll('style')) {
      if (el.innerHTML === '$DARK_MODE_CSS') {
        el.remove()
      }
    }
  })()
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomWebView(
  url: String,
  state: WebViewState,
  modifier: Modifier = Modifier,
  isDarkMode: Boolean = isSystemInDarkTheme(),
  listenBack: Boolean = true,
  finish: () -> Unit,
  cookies: String? = null,
  extraScripts: String? = null
) {
  // 封装 TBS WebView 组件
  // 部分代码来自 https://stackoverflow.com/questions/34986413/location-is-accessed-in-chrome-doesnt-work-in-webview/34990055

  val ctx = LocalContext.current

  key(url) {
    AndroidView(
      factory = { context ->
        if (state.webView != null) {
          // 如果 webview 已经存在，则直接返回
          return@AndroidView state.webView!!
        } else {
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
                  super.onPageFinished(view, url)
                  if (isDarkMode) {
                    // 注入暗黑模式 css
                    view?.evaluateJavascript(
                      DARK_MODE_JS
                    ) {}
                  } else {
                    // 清除暗黑模式 css
                    view?.evaluateJavascript(
                      CLEAR_DARK_MODE_JS
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
          state.webView = webView

          // 注入 cookie
          if (cookies != null) {
            setCookieForDomain(url, cookies)
          }

          webView.loadUrl(url)
          return@AndroidView webView
        }
      },
      modifier = modifier.fillMaxSize()
    )
  }

  BackHandler {
    if (listenBack && state.webView?.canGoBack() == true) {
      state.webView?.goBack()
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