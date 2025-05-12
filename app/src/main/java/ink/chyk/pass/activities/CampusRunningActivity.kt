package ink.chyk.pass.activities

import android.Manifest
import android.content.pm.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.*
import androidx.lifecycle.*
import ink.chyk.pass.ui.theme.*
import com.tencent.smtt.sdk.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tencent.mmkv.MMKV
import ink.chyk.pass.*
import ink.chyk.pass.R
import ink.chyk.pass.web.*

class CampusRunningActivity : ComponentActivity() {
  // 校园乐跑小程序窗口

  // Declare permission launcher at class level
  private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

  // Permission state for the Composable to observe
  private val permissionState = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra("url")
    val mmkv = MMKV.defaultMMKV()

    // Register the launcher before the activity starts
    requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) {
      permissionState.value = it
    }

    // Check permission status initially
    checkLocationPermission()

    enableEdgeToEdge()

    setContent {
      if (url != null) {
        CampusRunningScreen(url, mmkv)
      } else {
        ArgsErrorScreen()
      }
    }
  }

  private fun checkLocationPermission() {
    ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION
    ).let {
      if (it == PackageManager.PERMISSION_GRANTED) {
        permissionState.value = true
      } else {
        permissionState.value = false
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
      }
    }
  }

  @Composable
  fun CampusRunningScreen(url: String, mmkv: MMKV) {
    // Use the permission state from the activity
    val permissionGranted by remember { permissionState }

    val webViewState = rememberWebViewState()

    AppTheme {
      Scaffold { innerPadding ->
        val alwaysDark = mmkv.decodeBool("campus_running_always_dark", false)
        val isDark = if (alwaysDark) true else isSystemInDarkTheme()

        if (permissionGranted) {
          key(url) {
            CustomWebView(
              url,
              state = webViewState,
              modifier = Modifier.padding(innerPadding),
              listenBack = false,
              finish = { finish() },
              isDarkMode = isDark,
            )
          }
        } else {
          PermissionGrantScreen(Modifier.padding(innerPadding))
        }
      }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleEvent = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }


    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        lifecycleEvent.value = event
        when (event) {
          Lifecycle.Event.ON_PAUSE -> webViewState.webView?.onPause()
          Lifecycle.Event.ON_RESUME -> webViewState.webView?.onResume()
          Lifecycle.Event.ON_DESTROY -> webViewState.webView?.destroy()
          else -> {}
        }
      }

      lifecycleOwner.lifecycle.addObserver(observer)

      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        webViewState.webView?.destroy()
      }
    }

    /*
    BackHandler {
      if (webViewState.value?.canGoBack() == true) {
        webViewState.value?.goBack()
      } else {
        webViewState.value?.destroy()
        finish()
      }
    }
     */
  }

  @Composable
  fun PermissionGrantScreen(modifier: Modifier = Modifier) {
    ErrorScreen(
      modifier,
      R.string.location_permission_required,
      R.string.location_permission_required_content
    )
  }

  @Composable
  fun ArgsErrorScreen(modifier: Modifier = Modifier) {
    ErrorScreen(modifier, R.string.args_error, R.string.args_error_content)
  }

  @Composable
  fun ErrorScreen(modifier: Modifier = Modifier, text1: Int, text2: Int) {
    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(id = R.drawable.neko1),
        contentDescription = "Neko",
      )
      Spacer(modifier = Modifier.height(32.dp))
      Text(
        text = stringResource(text1),
        style = MaterialTheme.typography.headlineMedium,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(text2),
        textAlign = TextAlign.Center,
      )
    }
  }
}