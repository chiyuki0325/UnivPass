package ink.chyk.pass.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import ink.chyk.pass.*
import ink.chyk.pass.ui.theme.*

class WebPageActivity : ComponentActivity() {
  // 采用腾讯 X5 浏览器内核模拟小程序运行环境

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra("url") ?: "http://localhost:8080"  // 生产环境自行替换 
    val scripts = intent.getStringExtra("scripts")
    val cookies = intent.getStringExtra("cookies")

    enableEdgeToEdge()
    setContent {
      AppTheme {
        Scaffold { innerPadding ->
          WebPageScreen(url, scripts, cookies, innerPadding)
        }
      }
    }
  }

  @Composable
  fun WebPageScreen(
    url: String,
    scripts: String?,
    cookies: String?,
    innerPadding: PaddingValues
  ) {
    CustomWebView(
      url = url,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      finish = { finish() },
      cookies = cookies,
      extraScripts = scripts
    )
  }
}