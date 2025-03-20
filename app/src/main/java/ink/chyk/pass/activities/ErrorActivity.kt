package ink.chyk.pass.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.tencent.mmkv.MMKV
import ink.chyk.pass.*
import ink.chyk.pass.R
import ink.chyk.pass.ui.theme.*
import kotlinx.coroutines.*

class ErrorActivity : ComponentActivity() {
  // 报错信息提示窗口

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val monitor = NetworkMonitor(this)
    val mmkv = MMKV.defaultMMKV()

    setContent {
      ErrorScreen(monitor, mmkv)
    }
  }

  @Composable
  fun ErrorScreen(monitor: NetworkMonitor, mmkv: MMKV) {
    val isConnected = monitor.isConnected.collectAsState()

    LaunchedEffect(Unit) {
      // 自动重试机制
      val isRetry = mmkv.decodeBool("retry", true)
      // 第一次跳错误界面时 默认重试
      if (isRetry) {
        mmkv.encode("retry", false)
        finish()
      } else {
        // 第二次再进入错误界面时，不再重试
        mmkv.encode("retry", true)
      }

      // 启用网络状态监视
      withContext(Dispatchers.IO) {
        monitor.startMonitoring()
      }
    }

    AppTheme {
      Scaffold { innerPadding ->
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Image(
                painter = painterResource(R.drawable.no_internet),
                contentDescription = "No Internet Icon",
                modifier = Modifier.size(32.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.request_failed_title),
                style = MaterialTheme.typography.headlineMedium,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.request_failed_content),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(
              painter = painterResource(R.drawable.potato_server),
              contentDescription = "Potato Server",
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
              onClick = {
                finish()
              },
              modifier = Modifier.width(96.dp),
              enabled = isConnected.value
            ) {
              Text(stringResource(R.string.retry))
            }
          }
        }
      }
    }
  }
}