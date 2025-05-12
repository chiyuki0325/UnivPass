package ink.chyk.pass.screens

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.*
import androidx.compose.ui.window.Dialog
import androidx.navigation.*
import ink.chyk.pass.DialogTitle
import ink.chyk.pass.R
import ink.chyk.pass.activities.*
import ink.chyk.pass.screens.app_cards.*
import ink.chyk.pass.viewmodels.*
import kotlinx.serialization.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@ExperimentalSerializationApi
@Composable
fun AppsScreen(
  viewModel: AppsViewModel,
  @Suppress("UNUSED_PARAMETER")
  navController: NavController,
  innerPadding: PaddingValues
) {
  val ctx = LocalContext.current
  val appCardsOrder by viewModel.appCardsOrder.collectAsState()
  var showSortAppsDialog by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(16.dp),
  ) {
    LazyColumn {
      item {
        AppsHeader { showSortAppsDialog = true }
      }
      appCardsOrder.forEach {
        if (it.second) {
          item(key = it.first) {
            Spacer(modifier = Modifier.height(16.dp))
            AppCard(it.first, viewModel, Modifier.animateItem())
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
          onClick = {
            val intent = Intent(ctx, MoreAppsActivity::class.java)
            ctx.startActivity(intent)
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(R.string.more))
        }
      }
    }
  }

  if (showSortAppsDialog) {
    SortAppsDialog(viewModel, onDismiss = { showSortAppsDialog = false })
  }
}

@ExperimentalSerializationApi
@Composable
fun AppCard(key: String, viewModel: AppsViewModel, modifier: Modifier) {
  Box(modifier) {
    when (key) {
      "campus_run" -> CampusRunCard(viewModel)
      "mail_box" -> MailBoxCard(viewModel)
      "ip_gateway" -> IpGatewayCard(viewModel)
    }
  }
}


@Composable
fun AppsHeader(
  showSortAppsDialog: () -> Unit
) {
  // 顶部间距和标题
  Spacer(modifier = Modifier.height(32.dp))
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = stringResource(R.string.apps_center),
      style = MaterialTheme.typography.headlineLarge,
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(
      onClick = showSortAppsDialog,
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_fluent_arrow_sort_24_regular),
        contentDescription = null,
      )
    }
  }
  Spacer(modifier = Modifier.height(16.dp))
}


@ExperimentalSerializationApi
@Composable
fun SortAppsDialog(
  viewModel: AppsViewModel,
  onDismiss: () -> Unit
) {
  // github.com/Calvin-LL/Reorderable

  @Composable
  fun appName(appId: String): String {
    return when (appId) {
      "campus_run" -> stringResource(R.string.campus_run)
      "mail_box" -> stringResource(R.string.student_mailbox)
      "ip_gateway" -> stringResource(R.string.ip_gateway)
      else -> ""
    }
  }

  @Composable
  fun appIcon(appId: String): Int {
    return when (appId) {
      "campus_run" -> R.drawable.ic_fluent_run_48_regular
      "mail_box" -> R.drawable.ic_fluent_mail_48_regular
      "ip_gateway" -> R.drawable.ic_fluent_desktop_signal_24_regular
      else -> R.drawable.ic_fluent_arrow_sort_24_regular
    }
  }

  var list by remember { mutableStateOf(viewModel.getAppCardsOrder()) }
  val lazyListState = rememberLazyListState()
  val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    list = list.toMutableList().apply {
      add(to.index, removeAt(from.index))
    }
    viewModel.setAppCardsOrder(list)
  }

  Dialog(onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .clip(RoundedCornerShape(8.dp))
      ) {
        DialogTitle(
          R.drawable.ic_fluent_arrow_sort_24_regular,
          stringResource(R.string.sort_apps)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
          state = lazyListState,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(list, key = { it.first }) {
            ReorderableItem(reorderableLazyListState, key = it.first) { isDragging ->
              val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
              Surface(
                shadowElevation = elevation,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shape = RoundedCornerShape(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier
                    .draggableHandle()
                  // .padding(8.dp)
                ) {
                  val id = it.first
                  val enabled = it.second
                  Checkbox(
                    checked = enabled,
                    onCheckedChange = { newEnabled ->
                      list = list.toMutableList().apply {
                        val index = indexOfFirst { it.first == id }
                        if (index != -1) {
                          this[index] = id to newEnabled
                        }
                      }
                      viewModel.setAppCardsOrder(list)
                    }
                  )
                  Icon(
                    painter = painterResource(appIcon(it.first)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                  )
                  Text(appName(it.first))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 提示文字

        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = stringResource(R.string.reorder_details_1),
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = stringResource(R.string.reorder_details_2),
            style = MaterialTheme.typography.bodyMedium
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(R.string.done))
        }
      }
    }
  }
}