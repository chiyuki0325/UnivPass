package ink.chyk.pass.screens.app_cards

import android.annotation.*
import android.util.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import ink.chyk.pass.*
import ink.chyk.pass.R
import ink.chyk.pass.viewmodels.*
import kotlinx.serialization.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*

@SuppressLint("DefaultLocale")
private fun Long.toMB(): String {
  // 将字节转换为 MB
  return String.format("%.2f MB", this / 1024.0 / 1024.0)
}

private val fontSize = 15.sp

@SuppressLint("DefaultLocale")
@ExperimentalSerializationApi
@Composable
fun IpGatewayCard(
  viewModel: AppsViewModel
) {
  // 深澜网关认证卡片

  val loadingState by viewModel.ipGatewayState.collectAsState()
  val gatewayInfo by viewModel.gatewayInfo.collectAsState()
  var onlineDevices by remember { mutableIntStateOf(0) } // 在线设备数
  val ctx = LocalContext.current
  val networkMonitor = NetworkMonitor(ctx)

  LaunchedEffect(Unit) {
    // 加载网关数据
    viewModel.initIpGateway()
    // 启用网络状态监视
    withContext(Dispatchers.IO) {
      networkMonitor.startMonitoring()
    }
  }

  // 监听 isConnected 状态，变化时 initIpGateway
  val isConnected by networkMonitor.isConnected.collectAsState()
  LaunchedEffect(isConnected) {
    if (isConnected) {
      viewModel.initIpGateway()
    }
  }

  BaseAppCard(
    icon = R.drawable.ic_fluent_desktop_signal_24_regular,
    title = stringResource(R.string.ip_gateway),
    subtitle = buildAnnotatedString {
      when (loadingState) {
        LoadingState.LOADING -> append(stringResource(R.string.loading))
        LoadingState.FAILED -> append(stringResource(R.string.not_campus_network))
        LoadingState.SUCCESS -> when (gatewayInfo?.isOnline) {
          true -> {
            onlineDevices = gatewayInfo?.onlineInfo?.onlineDeviceTotal?.toInt() ?: 0
            append(stringResource(R.string.current_online_ip))
            append(": ")
            append(gatewayInfo?.onlineInfo?.onlineIp ?: stringResource(R.string.loading))
          }

          false -> append(stringResource(R.string.gateway_logged_out))
          null -> append(stringResource(R.string.request_failed_title))
        }

        LoadingState.PARTIAL_SUCCESS -> stringResource(R.string.failed_to_fetch_user_data)
      }
    },
    hasJumpButton = true,
    jumpButtonIcon = if (gatewayInfo?.isOnline == true) R.drawable.ic_fluent_link_dismiss_24_filled
    else R.drawable.ic_fluent_link_add_24_filled,
    jumpButtonClick = {
      if (gatewayInfo?.isOnline == true) {
        // 断开网关
        viewModel.logoutIpGateway()
      } else {
        // 登录网关
        viewModel.loginIpGateway()
      }
    },
    jumpButtonEnabled = loadingState == LoadingState.SUCCESS
  ) {
    if (loadingState == LoadingState.SUCCESS && gatewayInfo?.isOnline == true) {
      // 加载成功后显示网络详细信息
      Spacer(modifier = Modifier.height(8.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .animateContentSize(),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          InfoLine(
            R.string.traffic_used,
            gatewayInfo?.onlineInfo?.sumBytes?.toMB() ?: stringResource(R.string.loading)
          )
          InfoLine(
            R.string.traffic_left,
            gatewayInfo?.onlineInfo?.remainBytes?.toMB() ?: stringResource(R.string.loading)
          )
        }
        InfoLine(
          R.string.network_balance,
          "${
            String.format(
              "%.2f",
              gatewayInfo?.onlineInfo?.userBalance ?: 0.00
            )
          } ${stringResource(R.string.yuan)}"
        )

        Text(fontSize = fontSize, text = gatewayInfo?.onlineInfo?.billingName ?: "")

        if (onlineDevices > 1) {
          // 共几台设备在线
          Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = fontSize)) {
              append(stringResource(R.string.devices_online_1))
              append(' ')
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(onlineDevices.toString())
              }
              append(' ')
              append(stringResource(R.string.devices_online_2))
            }
          })
        }
      }
    }
  }
}

// 一行信息
@Composable
private fun InfoLine(k: Int, v: String) {
  Text(buildAnnotatedString {
    withStyle(SpanStyle(fontSize = fontSize)) {
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(k)) }
      append(" ")
      append(v)
    }
  })
}