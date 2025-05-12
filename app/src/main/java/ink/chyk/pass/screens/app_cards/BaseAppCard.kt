package ink.chyk.pass.screens.app_cards

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

@Composable
    /** AppsScreen 中应用卡片的基础组件
     * @param content 卡片内容
     */
fun BaseAppCard(
  icon: Int,
  title: String,
  subtitle: AnnotatedString,
  hasJumpButton: Boolean = true,
  jumpButtonEnabled: Boolean = true,
  jumpButtonIcon: Int = 0,
  jumpButtonClick: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.animateContentSize(),
  ) {
    Column(
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth(),
    ) {

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // 应用图标
        Icon(
          painter = painterResource(id = icon),
          contentDescription = null,
          modifier = Modifier.size(48.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
        ) {
          // 应用标题
          Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          // 第二行 下面的灰字
          Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
          )
        }
        if (hasJumpButton) {
          key(jumpButtonEnabled) {
            IconButton(
              enabled = jumpButtonEnabled,
              onClick = jumpButtonClick,
              colors = IconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondary,
              ),
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                painter = painterResource(id = jumpButtonIcon),
                contentDescription = title,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        }
        // 第一行结束
      }

      // 第二行开始为各个应用的内容
      content()
    }
  }
}