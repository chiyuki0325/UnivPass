package ink.chyk.pass.screens.courses

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import dev.darkokoa.pangu.Pangu
import ink.chyk.pass.*
import ink.chyk.pass.R
import ink.chyk.pass.api.*
import ink.chyk.pass.viewmodels.CoursesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.format.TextStyle as TimeTextStyle
import java.util.Locale
import kotlin.collections.forEach
import kotlin.math.*
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*

// 早午晚图标常量
val MORNING_ICON = R.drawable.ic_fluent_weather_sunny_24_regular
val AFTERNOON_ICON = R.drawable.ic_fluent_weather_sunny_low_24_regular
val EVENING_ICON = R.drawable.ic_fluent_weather_partly_cloudy_night_24_regular

@Composable
fun TodayCourses(
  viewModel: CoursesViewModel,
  dateState: MutableStateFlow<String>,
  switchView: () -> Unit
) {
  // 课表主组件
  // 其中的 dateState 由最顶级组件传递

  val showWeekJumpDialog = remember { mutableStateOf(false) }

  val density = LocalDensity.current
  var width by remember { mutableIntStateOf(0) }
  val widthDp = with(density) { width.toDp() }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .onSizeChanged { size ->
        width = size.width
      }
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      // 日期和每日一言
      TodayTitle(viewModel, dateState, switchView)
      Spacer(modifier = Modifier.height(16.dp))

      // 课程卡片封装组件
      CoursesCardOuter(viewModel, dateState, widthDp)
    }
    // 底部选择器
    DaySelector(viewModel, showJumpDialog = { showWeekJumpDialog.value = true }, dateState)
  }

  // 日期跳转对话框
  if (showWeekJumpDialog.value) {
    WeekJumpDialog(
      viewModel = viewModel,
      onDismissRequest = { showWeekJumpDialog.value = false },
      dateState = dateState
    )
  }
}

@Composable
fun CoursesCardOuter(
  viewModel: CoursesViewModel,
  dateState: MutableStateFlow<String>,
  width: Dp,
) {
  // 课程卡片封装组件（左右滑动翻页）

  // 当前的日期
  val date by dateState.collectAsState()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val widthPx = with(density) { width.toPx() }
  val spacingPx = with(density) { 4.dp.toPx() }

  // 先前 dateState 的值
  var previousDate by remember { mutableStateOf(date) }

  // 卡片上的日期数据
  var leftDate by remember { mutableStateOf(viewModel.prevDay(date)) }
  var middleDate by remember { mutableStateOf(date) }
  var rightDate by remember { mutableStateOf(viewModel.nextDay(date)) }

  var offsetX by remember { mutableFloatStateOf(0f) }
  var draggingOffsetX by remember { mutableFloatStateOf(0f) }
  var dateAnimating by remember { mutableStateOf(false) }
  var lastAnimateTarget by remember { mutableFloatStateOf(0f) }
  var animationJob by remember { mutableStateOf<Job?>(null) }


  suspend fun animateTo(
    targetPx: Float,
  ) {
    lastAnimateTarget = targetPx
    // 通过逐渐修改 offsetX 来移动课表卡片
    animate(
      initialValue = offsetX,
      targetValue = targetPx,
      animationSpec = tween(durationMillis = 230)
    ) { value, _ -> offsetX = value }
  }

  // 处理外部 dateState 变化
  LaunchedEffect(date) {
    // dateState 更新
    if (date != middleDate) {
      // 卡片需要播放滑动动画
      // 当前 previousDate 为之前的日期，dateId 为新的 dateState 的值

      if (dateAnimating) {
        // 给先前的动画收尾
        animationJob?.cancel()
        /*
        animate(
          initialValue = offsetX,
          targetValue = lastAnimateTarget,
          animationSpec = tween(durationMillis = 10)
        ) { value, _ -> offsetX = value }
        // 可能会导致低性能设备卡顿
        // 某为：直接报我名得了
         */
        withContext(Dispatchers.Main) {
          leftDate = viewModel.prevDay(previousDate)
          middleDate = previousDate
          rightDate = viewModel.nextDay(previousDate)
          offsetX = 0f
        }
      }

      animationJob = scope.launch {
        dateAnimating = true
        if (date > previousDate) {
          // 新的日期在右侧，卡片向左滑动
          withContext(Dispatchers.Main) {
            leftDate = viewModel.prevDay(previousDate)
            middleDate = previousDate
            rightDate = date
          }
          // 播放动画
          animateTo(-widthPx - spacingPx)
          // 动画完毕，恢复左中右日期和 offsetX
          withContext(Dispatchers.Main) {
            leftDate = viewModel.prevDay(date)
            middleDate = date
            rightDate = viewModel.nextDay(date)
            offsetX = 0f
          }
        } else if (date < previousDate) {
          // 新的日期在左侧，卡片向右滑动
          withContext(Dispatchers.Main) {
            leftDate = date
            middleDate = previousDate
            rightDate = viewModel.nextDay(previousDate)
          }
          // 播放动画
          animateTo(widthPx + spacingPx)
          // 动画完毕，恢复左中右日期和 offsetX
          withContext(Dispatchers.Main) {
            leftDate = viewModel.prevDay(date)
            middleDate = date
            rightDate = viewModel.nextDay(date)
            offsetX = 0f
          }
        }
        previousDate = date
        dateAnimating = false
      }
    }
  }


  val dragModifier = Modifier.draggable(
    orientation = Orientation.Horizontal,
    state = rememberDraggableState { delta ->
      offsetX += delta
      draggingOffsetX += delta
    },
    // enabled = !animating,
    onDragStopped = { velocity ->
      when {
        draggingOffsetX < -widthPx / 5 -> {
          // 下一天
          dateState.value = viewModel.nextDay(date)
        }

        draggingOffsetX > widthPx / 5 -> {
          // 上一天
          dateState.value = viewModel.prevDay(date)
        }

        else -> {
          // 回弹
          animateTo(0f)
        }
      }
      draggingOffsetX = 0f
    }
  )

  Box(
    modifier = Modifier
      .width(width)
      .fillMaxHeight()
      .then(dragModifier)
      .clipToBounds()
  ) {
    // 前一页
    CoursesCard(
      viewModel, leftDate,
      modifier = Modifier
        .width(width)
        .offset { IntOffset((offsetX - widthPx - spacingPx).roundToInt(), 0) }
    )

    // 当前页
    CoursesCard(
      viewModel, middleDate,
      modifier = Modifier
        .width(width)
        .offset { IntOffset(offsetX.roundToInt(), 0) }
    )

    // 后一页
    CoursesCard(
      viewModel, rightDate,
      modifier = Modifier
        .width(width)
        .offset { IntOffset((offsetX + widthPx + spacingPx).roundToInt(), 0) }
    )
  }
}

@Composable
fun TodayTitle(
  viewModel: CoursesViewModel,
  dateState: MutableStateFlow<String>,
  switchView: () -> Unit
) {
  val date by dateState.collectAsState()
  val ctx = LocalContext.current
  var quote by remember { mutableStateOf<Pair<String, String>?>(null) }

  LaunchedEffect(Unit) {
    // 界面加载时，获取每日一言
    if (quote == null) {
      quote = Hitokoto.getQuote(ctx)
    }
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      ctx.getString(R.string.date).format(
        date.slice(4..5).toInt(),  // 月
        date.slice(6..7).toInt(),  // 日
        viewModel.getWeekday(date)
      ),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(
      onClick = switchView
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_calendar_month_24_regular),
        contentDescription = "周视图",
      )
    }
  }
  Spacer(modifier = Modifier.height(8.dp))
  FoldInTextAnimation(quote)
}

@Composable
fun CoursesCard(
  viewModel: CoursesViewModel,
  dateId: String,
  modifier: Modifier = Modifier
) {
  // 课表卡片

  val todayCourses = viewModel.getCoursesByDate(dateId)
  val scrollState = rememberScrollState()

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(0.dp)
      .verticalScroll(scrollState),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
    ) {
      if (todayCourses.isEmpty()) {
        NoCoursesSplash(dateId)
      } else {
        TodayCoursesList(todayCourses)
      }
    }
  }
}


@Composable
fun FoldInTextAnimation(quote: Pair<String, String>?) {
  var isVisible by remember { mutableStateOf(false) }

  // 触发动画
  LaunchedEffect(Unit) {
    isVisible = true
  }

  Column(
    modifier = Modifier.fillMaxWidth()
  ) {
    AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start), // 从左往右展开
      exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start) // 从右往左折叠
    ) {
      Text(
        text = quote?.first ?: "",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.animateContentSize() // 内容大小变化的动画
      )
    }

    AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
      exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
    ) {
      Text(
        text = quote?.second?.let { "—— $it" } ?: "",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        modifier = Modifier.animateContentSize()
      )
    }
  }
}

@Composable
fun TodayCoursesList(
  todayCourses: List<Course>,
  dark: Boolean = isSystemInDarkTheme()
) {
  val ctx = LocalContext.current
  var previousIcon = 0  // 用于判断是否需要显示早午晚标题

  var lastEnd = -1

  todayCourses.forEach {
    val icon = when (it.period) {
      CoursePeriod.MORNING -> MORNING_ICON
      CoursePeriod.AFTERNOON -> AFTERNOON_ICON
      CoursePeriod.EVENING -> EVENING_ICON
    }

    if (icon != previousIcon) {
      // 课程区间变化了，显示早午晚标题

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // 早午晚标题
        Text(
          when (it.period) {
            CoursePeriod.MORNING -> ctx.getString(R.string.morning_courses)
            CoursePeriod.AFTERNOON -> ctx.getString(R.string.afternoon_courses)
            CoursePeriod.EVENING -> ctx.getString(R.string.evening_courses)
          },
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1f))

        // 这是一条可爱的分割线
        Surface(
          modifier = Modifier
            .height(1.dp)
            .fillMaxWidth(),
          color = if (dark) Color.DarkGray else Color.LightGray
        ) {}
      }
      previousIcon = icon
    }

    val isOverlapped = it.st <= lastEnd

    Box(
      modifier = Modifier
        .height(64.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // 课程开始和结束时间
          Column(modifier = Modifier.width(IntrinsicSize.Min)) {
            Text(
              it.start,
              fontFamily = FontFamily(Font(R.font.roboto_numeric)),
              fontWeight = FontWeight.Medium,
              color = if (isOverlapped) Color.Red else Color.Unspecified
            )
            Text(
              it.end,
              fontFamily = FontFamily(Font(R.font.roboto_numeric)),
              fontWeight = FontWeight.Medium,
              color = if (isOverlapped) Color.Red else Color.Unspecified
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          // 课程颜色
          Surface(
            modifier = Modifier
              .width(4.dp)
              .height(40.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = Color(HashColorHelper.calcColor(it.name))
          ) {}
          Spacer(modifier = Modifier.width(8.dp))
          // 使用剩余权重，截断多余文本，显示省略号
          Column(modifier = Modifier.weight(1f)) {
            // 课程名称
            Text(
              (if (isOverlapped) stringResource(R.string.course_overlapped) + " " else "") + Pangu.spacingText(
                it.name
              ),
              style = TextStyle(
                fontWeight = if (isOverlapped) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 20.sp
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            // 课程地点
            Text(
              locationToAnnotated(it.location),
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          painter = painterResource(icon),
          contentDescription = "Time Delimiter",
        )
      }
    }

    lastEnd = it.en
  }
}

@Composable
fun DaySelector(
  viewModel: CoursesViewModel,
  showJumpDialog: () -> Unit,
  dateState: MutableStateFlow<String>,
  dark: Boolean = isSystemInDarkTheme(),
) {
  val ctx = LocalContext.current
  val dateId by dateState.collectAsState()
  val thisWeekDates = viewModel.thisWeekDates(dateId)
  val selectedIndex = thisWeekDates.indexOfFirst { it.first.second == dateId }
  var itemSize by remember { mutableStateOf(0) }

  val density = LocalDensity.current
  val padding4DpAsPx = with(density) { 4.dp.toPx() }
  val interactionSource = remember { MutableInteractionSource() }

  // 高亮层偏移量（动画驱动）
  val highlightOffset by animateFloatAsState(
    targetValue = selectedIndex * (itemSize + padding4DpAsPx) + 5,  // 我也不知道为什么要+5 但是不加就会往左偏
    animationSpec = tween(durationMillis = 200)
  )

  Column {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(
          with(density) { itemSize.toDp() }
        ),
    ) {
      Box(
        modifier = Modifier
          .size(with(density) { itemSize.toDp() })
          .aspectRatio(1f)
          .offset { IntOffset(highlightOffset.roundToInt(), 0) }
          .clip(RoundedCornerShape(8.dp))
          .background(if (dark) Color.DarkGray else Color.LightGray)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        thisWeekDates.forEach {
          // 简单解包
          val (pack1, courseCount) = it
          val (thatDate, thatDateId) = pack1

          Box(
            modifier = Modifier
              .weight(1f) // 每个子项占相等比例的空间
              .aspectRatio(1f) // 保持正方形
              .padding(2.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable(
                interactionSource = interactionSource,
                indication = null
              ) {
                dateState.value = thatDateId
              }
              .onSizeChanged {
                itemSize = it.width
              },
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                thatDate.dayOfMonth.toString(),
                // style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                "${
                  thatDate.dayOfWeek.getDisplayName(
                    TimeTextStyle.SHORT,
                    Locale.getDefault()
                  )
                } $courseCount",
                style = TextStyle(fontSize = 8.sp)
              )
            }
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      PrevNextButton(
        icon = R.drawable.ic_fluent_chevron_left_20_filled,
        onClick = { dateState.value = viewModel.prevWeek(dateId) }
      )
      val thisWeekNum = viewModel.thisWeek(dateId)
      Text(
        if (thisWeekNum == -1) ctx.getString(R.string.in_vacation)
        else
          ctx.getString(
            if (viewModel.isToday(dateId)) R.string.current_week
            else R.string.current_week_navigated
          ).format(thisWeekNum),

        modifier = Modifier.clickable {
          showJumpDialog()
        }
      )

      if (!viewModel.isToday(dateId)) {
        Text(
          modifier = Modifier.clickable {
            dateState.value = viewModel.backToday()
          },
          text = ctx.getString(R.string.week_jump_today),
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold
        )
      }

      PrevNextButton(
        icon = R.drawable.ic_fluent_chevron_right_20_filled,
        onClick = { dateState.value = viewModel.nextWeek(dateId) }
      )
    }
  }
}

@Composable
fun PrevNextButton(
  icon: Int,
  onClick: () -> Unit
) {
  IconButton(
    onClick = onClick
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = "Prev/Next Button",
    )
  }
}

@Composable
fun NoCoursesSplash(dateId: String) {
  val ctx = LocalContext.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(48.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      painter = painterResource(
        listOf(
          R.drawable.neko1,
          R.drawable.neko2,
          R.drawable.neko3
        )[dateId.hashCode().absoluteValue % 3]
      ),
      contentDescription = "No Courses",
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      ctx.getString(R.string.no_course),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = Modifier.height(16.dp))
    ImportCoursesButton()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekJumpDialog(
  viewModel: CoursesViewModel,
  onDismissRequest: () -> Unit,
  dateState: MutableStateFlow<String>
) {
  val datePickerState = rememberDatePickerState(
    selectableDates = object : SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val selectedDate = java.time.LocalDate.ofEpochDay(utcTimeMillis / 86400000)
          .format(viewModel.formatter)
        return viewModel.isDateInTerm(selectedDate)
      }
    }
  )
  val selectedDate = datePickerState.selectedDateMillis?.let {
    val date = java.time.LocalDate.ofEpochDay(it / 86400000)
    date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
  } ?: ""

  // 切换至第几周的对话框
  Dialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismissRequest
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .clip(RoundedCornerShape(8.dp))
      ) {
        DialogTitle(
          R.drawable.ic_fluent_calendar_24_regular,
          stringResource(R.string.week_jump)
        )

        Spacer(modifier = Modifier.height(8.dp))

        DatePicker(
          state = datePickerState,
          showModeToggle = false,
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
        )


        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = {
              dateState.value = viewModel.backToday()
              onDismissRequest()
            }
          ) {
            Text(stringResource(R.string.week_jump_today))
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              onDismissRequest()
            }
          ) {
            Text(stringResource(R.string.cancel))
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              if (viewModel.isDateInTerm(selectedDate)) {
                dateState.value = selectedDate
                onDismissRequest()
              }
            }
          ) {
            Text(stringResource(R.string.confirm))
          }
        }
      }
    }
  }
}

fun locationToAnnotated(text: String): AnnotatedString {
    return if (text.endsWith("校区)")) { 
    val location = text.substringBefore(" (")
    val campus = text.substringAfter("(").substringBefore(")")
    buildAnnotatedString {
      withStyle(style = SpanStyle(fontSize = 14.sp)) {
        append(location)
      }
      withStyle(
        style = SpanStyle(
          fontSize = 14.sp,
          color = Color.Gray
        )
      ) {
        append("（")
        append(campus)
        append("）")
      }
    }
  } else {
    buildAnnotatedString {
      withStyle(style = SpanStyle(fontSize = 14.sp)) {
        append(text)
      }
    }
  }
}