package ink.chyk.pass.activities

import android.app.*
import android.content.*
import android.os.*
import android.util.*
import android.widget.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.tencent.mmkv.*
import com.tencent.smtt.export.external.TbsCoreSettings.*
import com.tencent.smtt.sdk.QbSdk.*
import ink.chyk.pass.R
import ink.chyk.pass.screens.*
import ink.chyk.pass.ui.theme.*
import ink.chyk.pass.viewmodels.*
import kotlinx.serialization.*

class MainActivity : ComponentActivity() {
  // Universal Pass 主窗口

  @ExperimentalSerializationApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 初始化 mmkv
    MMKV.initialize(this)

    val mmkv = MMKV.defaultMMKV()
    val screen = intent.getStringExtra("screen")
    Log.d("MainActivity", "screen: $screen")

    // 检查是否登录？
    if (!mmkv.containsKey("student_id")) {
      Toast.makeText(this, "请先登录校园账号。", Toast.LENGTH_SHORT).show()
      val intent = Intent(this, LoginActivity::class.java)
      startActivity(intent)
      finish()
      return
    }

    // 检查是否同意服务条款？
    if ((!mmkv.containsKey("tos")) || (mmkv.decodeInt("tos") < 400)) {
      if (mmkv.containsKey("tos") && mmkv.decodeInt("tos") < 400) mmkv.remove("student_id")
      // 更新到 400 使用条款后 退出登录
      val intent = Intent(this, TermsOfServiceActivity::class.java)
      startActivity(intent)
      finish()
      return
    }

    // 加载 Tencent Browsing Service
    val context = this.applicationContext
    setDownloadWithoutWifi(true)  // 学生应该都有流量卡
    // 根据学校情况决定是否启用 setDownloadWithoutWifi
    // 在调用TBS初始化、创建WebView之前进行如下配置
    initTbsSettings(
      hashMapOf(
        TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true,
        TBS_SETTINGS_USE_DEXLOADER_SERVICE to true
      ) as Map<String, Boolean>
    )
    initX5Environment(context, object : PreInitCallback {
      override fun onCoreInitFinished() {
        Log.d("MainActivity", "X5 Core Init Finished")
      }
      override fun onViewInitFinished(isX5: Boolean) {
        Log.d("MainActivity", "X5 View Init Finished: $isX5")
      }
    })


    enableEdgeToEdge()
    setContent {
      MainApp(screen)
    }
  }
}


data class BottomNavigationItem(
  val label: String = "",
  val icon: Int = 0,
  val route: String = ""
) {
  fun navigationItems(): List<BottomNavigationItem> {
    return listOf(
      BottomNavigationItem(
        "课表",
        R.drawable.ic_fluent_calendar_24_filled,
        "courses"
      ),
      BottomNavigationItem(
        通行码 
        R.drawable.ic_fluent_qr_code_24_filled,
        "passcode"
      ),
      BottomNavigationItem(
        "应用",
        R.drawable.ic_fluent_apps_24_filled,
        "apps"
      ),
      BottomNavigationItem(
        "我的",
        R.drawable.ic_fluent_person_24_filled,
        "profile"
      )
    )
  }
}

fun routeToIndex(route: String): Int {
  return when (route) {
    "courses" -> 0
    "passcode" -> 1
    "apps" -> 2
    "profile" -> 3
    else -> 1
  }
}

fun enter(
  previousItem: Int = 0,
  selectedItem: Int = 0
): EnterTransition {
  return if (previousItem < selectedItem) {
    slideInHorizontally(
      initialOffsetX = { it },
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
      )
    )
  } else {
    slideInHorizontally(
      initialOffsetX = { -it },
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
      )
    )
  }
}

fun exit(
  previousItem: Int = 0,
  selectedItem: Int = 0
): ExitTransition {
  return if (previousItem < selectedItem) {
    slideOutHorizontally(
      targetOffsetX = { -it },
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
      )
    )
  } else {
    slideOutHorizontally(
      targetOffsetX = { it },
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
      )
    )
  }
}

@Composable
@ExperimentalSerializationApi
fun MainApp(screen: String?) {
  val ctx = LocalContext.current
  val onFailed = {
    // 检查应用是否在前台
    val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val appProcesses = activityManager.runningAppProcesses ?: emptyList()
    val isAppInForeground = appProcesses.any {
      it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
        it.processName == ctx.packageName
    }

    // 只有在前台时才显示错误，防止锁屏或者后台
    if (isAppInForeground) {
      val intent = Intent(ctx, ErrorActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      ctx.startActivity(intent)
    }

    true
  }
  val passCodeViewModel: PassCodeViewModel = viewModel(factory = PassCodeViewModelFactory())
  val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(onFailed))
  val coursesViewModel: CoursesViewModel = viewModel(factory = CoursesViewModelFactory())
  val appsViewModel: AppsViewModel = viewModel(factory = AppsViewModelFactory({ false }))

  val navController = rememberNavController()
  var previousSelectedItem by remember { mutableIntStateOf(0) }
  var selectedItem by remember { mutableIntStateOf(0) }

  LaunchedEffect(navController) {
    navController.currentBackStackEntryFlow.collect { backStackEntry ->
      // 根据当前的目的地更新底部导航栏的选中项
      selectedItem = routeToIndex(backStackEntry.destination.route ?: "passcode")
    }
  }

  AppTheme {
    Scaffold(
      bottomBar = {
        NavigationBar {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp)
          ) {
            BottomNavigationItem().navigationItems().forEachIndexed { index, item ->
              NavigationBarItem(
                selected = index == selectedItem,
                label = { Text(item.label) },
                icon = {
                  Icon(
                    painter = painterResource(item.icon),
                    contentDescription = item.label
                  )
                },
                onClick = {
                  if (selectedItem != index) {
                    previousSelectedItem = selectedItem
                    selectedItem = index
                    navController.navigate(item.route) {
                      // https://medium.com/@bharadwaj.rns/bottom-navigation-in-jetpack-compose-using-material3-c153ccbf0593
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                }
              )
            }
          }
        }
      }
    ) { innerPadding ->
      NavHost(navController = navController, startDestination = screen ?: "passcode") {
        composable(
          "courses",
          enterTransition = { enter(previousSelectedItem, selectedItem) },
          exitTransition = { exit(previousSelectedItem, selectedItem) }
        ) {
          CoursesScreen(
            viewModel = coursesViewModel,
            navController = navController,
            innerPadding = innerPadding
          )
        }
        composable(
          "passcode",
          enterTransition = { enter(previousSelectedItem, selectedItem) },
          exitTransition = { exit(previousSelectedItem, selectedItem) }
        ) {
          PassCodeScreen(viewModel = passCodeViewModel, navController = navController)
        }
        composable(
          "apps",
          enterTransition = { enter(previousSelectedItem, selectedItem) },
          exitTransition = { exit(previousSelectedItem, selectedItem) }
        ) {
          AppsScreen(
            viewModel = appsViewModel,
            navController = navController,
            innerPadding = innerPadding
          )
        }
        composable(
          "profile",
          enterTransition = { enter(previousSelectedItem, selectedItem) },
          exitTransition = { exit(previousSelectedItem, selectedItem) }
        ) {
          ProfileScreen(
            viewModel = profileViewModel,
            navController = navController,
            innerPadding = innerPadding
          )
        }
      }
    }
  }
}