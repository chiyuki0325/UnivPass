package ink.chyk.pass.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.*
import ink.chyk.pass.ui.theme.*
import ink.chyk.pass.viewmodels.*
import ink.chyk.pass.R
import kotlinx.serialization.*
import androidx.compose.ui.platform.LocalContext
import ink.chyk.pass.api.*
import ink.chyk.pass.screens.*

class MoreAppsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppTheme {
        Scaffold { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .padding(16.dp),
          ) {
            MoreApps()
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun MoreApps() {
  val ctx = LocalContext.current
  val viewModelFactory = AppsViewModelFactory({ true })
  val viewModel: AppsViewModel = viewModel(factory = viewModelFactory)
  val appCardsOrder by viewModel.appCardsOrder.collectAsState()
  val chunkedAppLinks = AppLinks.links.chunked(4)


  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = {
          // 返回首屏
          val activity = ctx as MoreAppsActivity
          activity.finish()
        },
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_fluent_chevron_left_20_filled),
          contentDescription = "Back",
        )
      }
      Text(
        text = stringResource(R.string.more_apps),
        style = MaterialTheme.typography.headlineLarge,
      )
    }

    LazyColumn {
      // 被隐藏的小程序应用卡片
      appCardsOrder.forEach {
        if (!it.second) {
          item(key = it.first) {
            Spacer(modifier = Modifier.height(8.dp))
            AppCard(it.first, viewModel, Modifier.animateItem())
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
      // 网页应用
      items(chunkedAppLinks) { rowAppLinks ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          rowAppLinks.forEach {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              IconButton(
                onClick = {
                  // 打开对应的应用网页
                  viewModel.openWebApp(ctx, it.redirectUrl)
                },
                colors = IconButtonColors(
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                  containerColor = MaterialTheme.colorScheme.primary,
                  disabledContentColor = MaterialTheme.colorScheme.onSecondary,
                  disabledContainerColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier.size(56.dp),
              ) {
                Icon(
                  painter = painterResource(id = it.icon),
                  contentDescription = stringResource(it.name),
                  modifier = Modifier.size(32.dp),
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                stringResource(it.name),
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}