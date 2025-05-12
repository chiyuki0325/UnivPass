package ink.chyk.pass.web

import androidx.compose.runtime.*
import com.tencent.smtt.sdk.WebView

@Stable
class WebViewState {
  internal var webView by mutableStateOf<WebView?>(null)
}