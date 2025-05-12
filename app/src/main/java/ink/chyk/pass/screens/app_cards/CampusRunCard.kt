package ink.chyk.pass.screens.app_cards

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.darkokoa.pangu.Pangu
import ink.chyk.pass.LoadingState
import ink.chyk.pass.R
import ink.chyk.pass.viewmodels.AppsViewModel
import kotlinx.serialization.ExperimentalSerializationApi


@ExperimentalSerializationApi
@Composable
fun CampusRunCard(
  viewModel: AppsViewModel,
) {
  LaunchedEffect(Unit) {
    viewModel.initCampusRun()
    // 加载校园跑数据
  }

  val context = LocalContext.current
  val loadingState = viewModel.campusRunState.collectAsState()
  val termName = viewModel.termName.collectAsState()
  val beforeRun = viewModel.beforeRun.collectAsState()
  val termRunRecord = viewModel.termRunRecord.collectAsState()

  BaseAppCard(
    // 校园跑图标
    icon = R.drawable.ic_fluent_run_48_regular,
    // 校园跑标题
    title = stringResource(R.string.campus_run),
    // 当前学期名字 或者 加载失败字样
    subtitle = buildAnnotatedString {
      append(
        when (loadingState.value) {
          LoadingState.LOADING, LoadingState.PARTIAL_SUCCESS -> stringResource(R.string.loading)
          LoadingState.FAILED -> stringResource(R.string.no_network)
          LoadingState.SUCCESS -> termName.value
            // 略加修改
            .replace("~", " ~ ")
            .replace("学年", "").let {
              Pangu.spacingText(it)
            }
        }
      )
    },
    // 还在 go 还在 go
    jumpButtonIcon = R.drawable.ic_golang,
    // partial success 时已经取得登录 url
    jumpButtonEnabled = loadingState.value == LoadingState.PARTIAL_SUCCESS || loadingState.value == LoadingState.SUCCESS,
    // 进入校园跑按钮
    jumpButtonClick = {
      viewModel.startCampusRun(context)
    }
  ) {
    if (loadingState.value == LoadingState.SUCCESS) {
      // 成功之后绘制剩余的内容
      val fontSize = 15.sp

      Spacer(modifier = Modifier.height(8.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .animateContentSize(),
      ) {
        Text(
          // 今日已有 123 名 xdx 完成跑步
          buildAnnotatedString {
            append("今日已有 ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append(beforeRun.value?.today_run_user_num.toString())
            }
            append(" 名同学完成跑步") 
          },
          fontSize = fontSize,
        )

        val distance = termRunRecord.value?.total_score_distance
        val pair = viewModel.distanceColorAndTextPair(distance)

        Row(
          verticalAlignment = Alignment.Top
        ) {
          Text(
            buildAnnotatedString {
              append("本学期有效跑步 ")
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(termRunRecord.value?.total_score_num)
              }
              append(" 次，")
            },
            fontSize = fontSize
          )

          Box(modifier = Modifier.wrapContentSize()) {
            // 用于存储文字的宽度
            var textWidth by remember { mutableIntStateOf(0) }

            Text(
              buildAnnotatedString {
                append("共 ")
                withStyle(
                  SpanStyle(
                    fontWeight = FontWeight.Bold,
                  )
                ) {
                  append(distance)
                }
                append(" 公里")
              },
              onTextLayout = { textLayoutResult: TextLayoutResult ->
                textWidth = textLayoutResult.size.width
              },
              fontSize = fontSize,
              modifier = Modifier.align(Alignment.CenterStart)
            )

            // 绘制下划线, generated by DeepSeek
            Canvas(
              modifier = Modifier
                .width(textWidth.dp) // 根据文字宽度设置 Canvas 宽度
                .height(2.dp)
                .align(Alignment.BottomStart)
            ) {
              // 计算下划线的位置
              val canvasHeight = size.height

              // 绘制下划线
              drawLine(
                color = pair.first,
                start = Offset(0f, canvasHeight / 2),
                end = Offset(textWidth.toFloat(), canvasHeight / 2),
                strokeWidth = 2.dp.toPx()
              )
            }
          }
        }

        // 鼓励语
        Text(stringResource(pair.second), fontSize = fontSize)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          // 七天进度
          beforeRun.value?.weekData?.list?.forEach {
            DailyProgressCircle(it.date, it.distance, modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}
// CampusRunCard end

@Composable
fun DailyProgressCircle(
  date: String,
  distance: String,
  modifier: Modifier = Modifier
) {
  // 七天进度
  // 参照流行的校园跑 App 设计
  var distanceFloat = distance.toFloat()
  if (distanceFloat > 5) {
    distanceFloat = 5f
  }
  val dailyMax = 5  // km

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        progress = {
          distanceFloat / dailyMax
        }
      )
      Text(
        text = distance,
        modifier = Modifier.align(Alignment.Center),
        fontSize = if (distance == "0") 14.sp else 12.sp
      )
    }
    Text(
      text = date, 
      fontSize = 12.sp,
    )
  }
}
