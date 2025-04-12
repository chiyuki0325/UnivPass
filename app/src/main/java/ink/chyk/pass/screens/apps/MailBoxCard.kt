package ink.chyk.pass.screens.apps

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.chyk.pass.HashColorHelper
import ink.chyk.pass.LoadingState
import ink.chyk.pass.R
import ink.chyk.pass.api.MailDetails
import ink.chyk.pass.viewmodels.AppsViewModel
import kotlinx.serialization.ExperimentalSerializationApi


@ExperimentalSerializationApi
@Composable
fun MailBoxCard(
  viewModel: AppsViewModel,
) {
  LaunchedEffect(Unit) {
    viewModel.initMailBox()
    // 加载邮箱数据
  }

  val context = LocalContext.current
  val mailList = viewModel.mailList.collectAsState()
  val loadingState = viewModel.mailListState.collectAsState()

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
        // 邮箱图标
        Icon(
          painter = painterResource(
            if (
              mailList.value?.num != "0" && loadingState.value == LoadingState.SUCCESS
            ) {
              R.drawable.ic_fluent_mail_unread_48_regular
            } else {
              R.drawable.ic_fluent_mail_48_regular
            }
          ),
          contentDescription = null,
          modifier = Modifier.size(48.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
        ) {
          // 邮箱标题
          Text(
            stringResource(R.string.student_mailbox),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          // 邮箱数量
          Text(
            text = when (loadingState.value) {
              LoadingState.LOADING -> buildAnnotatedString {
                append(stringResource(R.string.loading))
              }

              LoadingState.FAILED -> buildAnnotatedString {
                append(stringResource(R.string.no_network))
              }

              LoadingState.PARTIAL_SUCCESS, LoadingState.SUCCESS -> {
                buildAnnotatedString {
                  withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(mailList.value?.num)
                  }
                  append(stringResource(R.string.unreads))
                }
              }
            },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
          )
        }
        IconButton(
          onClick = {
            // 进入邮箱按钮
            viewModel.openCoreMail(context)
          },
          colors = IconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary,
          ),
          modifier = Modifier.size(48.dp),
          enabled = loadingState.value == LoadingState.SUCCESS,
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_fluent_mail_read_32_regular),
            contentDescription = null,
            modifier = Modifier.height(32.dp),
          )
        }
      } // 第一行结束

      if (mailList.value?.num != "0") {
        // 有未读邮件
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        ) {
          val dark = isSystemInDarkTheme()
          mailList.value?.list?.forEach {
            // 邮件列表
            Spacer(modifier = Modifier.height(8.dp))
            MailItem(it, dark)
          }
        }
      }
    }
  }
}

@Composable
fun MailItem(
  mailDetails: MailDetails,
  dark: Boolean
) {
  // CoreMail 邮件条目
  // 在一行内 显示subject和from（减淡颜色）
  // 溢出的话显示省略号
  Row (
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // 和课表类似，根据邮件主题计算颜色
    Surface(
      modifier = Modifier
        .width(4.dp)
        .height(16.dp)
        .clip(RoundedCornerShape(4.dp)),
      color = Color(HashColorHelper.calcColor(mailDetails.subject))
    ) {}
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
          append(mailDetails.subject)
        }
        append(" - ")
        withStyle(SpanStyle(color = Color.Gray)) {
          append(mailDetails.from)
        }
      },
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}