package ink.chyk.pass.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.navigation.*
import ink.chyk.pass.viewmodels.*
import ink.chyk.pass.screens.courses.ImportCoursesSplash
import ink.chyk.pass.screens.courses.TodayCourses
import ink.chyk.pass.screens.courses.WeekCourses
import kotlinx.coroutines.flow.*


@Composable
fun CoursesScreen(
  viewModel: CoursesViewModel,
  @Suppress("unused")
  navController: NavController,  // 没必要
  innerPadding: PaddingValues
) {
  // 课程表界面

  // 是否为周视图
  var isWeekView by remember { mutableStateOf(false) }
  val switchView = {
    isWeekView = !isWeekView
  }

  // date 状态放在最顶级组件 之后逐级传递
  val dateState = remember { MutableStateFlow<String>(viewModel.today) }

  if (!viewModel.isCourseImported()) {
    return ImportCoursesSplash()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Crossfade(
      targetState = isWeekView,
      animationSpec = tween(200),
      label = "CoursesScreen",
    ) { isWeekView ->
      if (isWeekView) {
        WeekCourses(viewModel, dateState, switchView)
      } else {
        TodayCourses(viewModel, dateState, switchView)
      }
    }
  }
}
