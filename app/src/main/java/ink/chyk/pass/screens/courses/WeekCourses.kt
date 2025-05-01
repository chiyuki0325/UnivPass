package ink.chyk.pass.screens.courses

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import ink.chyk.pass.*
import ink.chyk.pass.R
import ink.chyk.pass.api.*
import ink.chyk.pass.viewmodels.CoursesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

@Composable
fun WeekCourses(
  viewModel: CoursesViewModel,
  dateState: MutableStateFlow<String>,
  switchView: () -> Unit,
) {
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
    WeekTitle(viewModel, dateState, switchView)
    Spacer(modifier = Modifier.height(8.dp))
    WeekCoursesGridOuter(viewModel, dateState, width, widthDp)
  }
}

@Composable
private fun WeekTitle(
  viewModel: CoursesViewModel,
  dateState: MutableStateFlow<String>,
  switchView: () -> Unit
) {
  val date by dateState.collectAsState()
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      stringResource(R.string.current_week_overview).format(viewModel.thisWeek(date)),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(
      onClick = switchView
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_calendar_today_24_regular),
        contentDescription = "日视图",
      )
    }
  }
}

@Composable
private fun WeekCoursesGridOuter(
  viewModel: CoursesViewModel,
  dateState: MutableStateFlow<String>,
  widthPx: Int,
  widthDp: Dp
) {
  // 用三个 WeekCoursesGrid 实现虚拟滚动

  // 当前的日期
  val date by dateState.collectAsState()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
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
          leftDate = viewModel.prevWeek(previousDate)
          middleDate = previousDate
          rightDate = viewModel.nextWeek(previousDate)
          offsetX = 0f
        }
      }

      animationJob = scope.launch {
        dateAnimating = true
        if (date > previousDate) {
          // 新的日期在右侧，卡片向左滑动
          withContext(Dispatchers.Main) {
            leftDate = viewModel.prevWeek(previousDate)
            middleDate = previousDate
            rightDate = date
          }
          // 播放动画
          animateTo(-widthPx - spacingPx)
          // 动画完毕，恢复左中右日期和 offsetX
          withContext(Dispatchers.Main) {
            leftDate = viewModel.prevWeek(date)
            middleDate = date
            rightDate = viewModel.nextWeek(date)
            offsetX = 0f
          }
        } else if (date < previousDate) {
          // 新的日期在左侧，卡片向右滑动
          withContext(Dispatchers.Main) {
            leftDate = date
            middleDate = previousDate
            rightDate = viewModel.nextWeek(previousDate)
          }
          // 播放动画
          animateTo(widthPx + spacingPx)
          // 动画完毕，恢复左中右日期和 offsetX
          withContext(Dispatchers.Main) {
            leftDate = viewModel.prevWeek(date)
            middleDate = date
            rightDate = viewModel.nextWeek(date)
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
          dateState.value = viewModel.nextWeek(date)
        }

        draggingOffsetX > widthPx / 5 -> {
          // 上一天
          dateState.value = viewModel.prevWeek(date)
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
      .width(widthDp)
      .fillMaxHeight()
      .then(dragModifier)
      .clipToBounds()
  ) {
    // 前一页
    WeekCoursesGrid(
      viewModel, leftDate, widthDp,
      modifier = Modifier
        .offset { IntOffset((offsetX - widthPx - spacingPx).roundToInt(), 0) }
    )

    // 当前页
    WeekCoursesGrid(
      viewModel, middleDate, widthDp,
      modifier = Modifier
        .offset { IntOffset(offsetX.roundToInt(), 0) }
    )

    // 后一页
    WeekCoursesGrid(
      viewModel, rightDate, widthDp,
      modifier = Modifier
        .offset { IntOffset((offsetX + widthPx + spacingPx).roundToInt(), 0) }
    )
  }
}

private val GRID_HEIGHT = 64.dp  // 课程表格行高

@Composable
private fun WeekCoursesGrid(
  viewModel: CoursesViewModel,
  date: String,
  widthDp: Dp,
  modifier: Modifier = Modifier,
) {
  val thisWeekDates = viewModel.thisWeekDates(date)
  val thisWeekCourses = thisWeekDates.map { viewModel.getCoursesByDate(it.first.second) }
  val isDark = isSystemInDarkTheme()
  val scrollState = rememberScrollState()

  // 本周课程表格
  Column(
    modifier = modifier.width(widthDp)
  ) {
    Row(
      modifier = Modifier.width(widthDp),
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      // 表头：几月几日星期几
      thisWeekDates.forEach {
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = it.first.first.dayOfWeek.getDisplayName(
              java.time.format.TextStyle.SHORT,
              java.util.Locale.getDefault()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (it.first.second == date) FontWeight.Bold else FontWeight.Normal,
          )
          Text(
            text = "${it.first.first.monthValue}/${it.first.first.dayOfMonth}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (it.first.second == date) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .width(widthDp)
        .verticalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      thisWeekCourses.forEach { courses ->
        // 每天一列
        // 当天课程，根据课程长度决定每节课单元格高度
        var lastPeriod = CoursePeriod.MORNING.ordinal
        var lastEnd = -1

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          courses.forEach {
            val name = if (it.st < lastEnd + 1) {
              // 发生撞课
              "[撞课] ${it.name}"
            } else {
              // 没有撞课
              it.name
            }

            if (it.st > lastEnd + 1) {
              // 课程开始时间大于上一个课程结束时间，说明有间隔
              Spacer(modifier = Modifier.height(GRID_HEIGHT * (it.st - lastEnd - 1) + 2.dp * (it.st - lastEnd - 2)))
            }

            if (it.period.ordinal != lastPeriod) {
              // 课程时间段不一样，说明隔着午休 / 晚休
              repeat(it.period.ordinal - lastPeriod) {
                Spacer(modifier = Modifier.height(8.dp))
              }
              lastPeriod = it.period.ordinal
            }
            // 课程单元格
            Box(
              modifier = Modifier
                .height((GRID_HEIGHT) * (it.en - it.st + 1) + (2.dp) * (it.en - it.st))
                .fillMaxWidth()
                .background(
                  color = Color(HashColorHelper.calcBackgroundColor(it.name, isDark)),
                  shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(4.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = name,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(HashColorHelper.calcTextColor(it.name, isDark)),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = it.location,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(HashColorHelper.calcTextColor(it.name, isDark)),
                )
              }
            }
            lastEnd = it.en
          }
        }
      }
    }
  }
}