package ink.chyk.pass.activities

import android.content.*
import android.graphics.Color.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.*
import com.tencent.mmkv.*
import ink.chyk.pass.*
import ink.chyk.pass.viewmodels.*
import ink.chyk.pass.R
import ink.chyk.pass.api.*
import ink.chyk.pass.ui.theme.*

class ImportCoursesActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 初始化 mmkv
    MMKV.initialize(this)

    val lightTransparentStyle = SystemBarStyle.light(
      scrim = TRANSPARENT,
      darkScrim = TRANSPARENT
    )
    enableEdgeToEdge(
      statusBarStyle = lightTransparentStyle,
      navigationBarStyle = lightTransparentStyle
    )


    setContent {
      AppTheme {
        ImportCoursesScreen()
      }
    }
  }
}


@Composable
fun ImportCoursesScreen() {
  // 课程表导入窗口

  val ctx = LocalContext.current
  val viewModel = viewModel<ImportCoursesViewModel>(factory = ImportCoursesViewModelFactory(PassAPI( {
    // 导入失败
    false
  })))
  val importing = viewModel.importing.collectAsState()
  val importCompleted = viewModel.importCompleted.collectAsState()
  val output = viewModel.output.collectAsState()
  val hasErrors = viewModel.hasErrors.collectAsState()

  Scaffold { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          SchoolTitle(ctx.getString(R.string.import_courses))
          Spacer(modifier = Modifier.height(16.dp))
          if (!importCompleted.value) {
            ImportContent(
              importing = importing,
              onClick = {
                viewModel.runImport()
              }
            )
          } else {
            AfterImportContent(
              output = output,
              hasErrors = hasErrors
            )
          }
        }
      }
    }
  }
}

@Composable
fun ImportContent(
  importing: State<Boolean>,
  onClick: () -> Unit
) {
  val ctx = LocalContext.current
  Text(
    ctx.getString(R.string.import_courses_content_1)
  )
  Text(
    buildAnnotatedString {
      append(ctx.getString(R.string.import_courses_content_2))
      withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(ctx.getString(R.string.import_courses_content_3))
      }
      append(ctx.getString(R.string.import_courses_content_4))
    }
  )
  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = onClick,
    enabled = !importing.value,
    modifier = Modifier.fillMaxWidth()
  ) {
    if (importing.value) {
      CircularProgressIndicator()
    } else {
      Text(ctx.getString(R.string.import_courses))
    }
  }
}

@Composable
fun AfterImportContent(
  output: State<String>,
  hasErrors: State<Boolean>
) {
  val scrollState = rememberScrollState()
  val ctx = LocalContext.current

  OutlinedTextField(
    value = output.value,
    onValueChange = {},
    label = { Text(ctx.getString(R.string.logs)) },
    readOnly = true,
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .height(400.dp)
  )
  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = {
      val intent = Intent(ctx, MainActivity::class.java)
      ctx.startActivity(intent)
    },
    modifier = Modifier.fillMaxWidth(),
  ) {
    if (hasErrors.value) {
      Text(stringResource(R.string.cancel))
    } else {
      Text(stringResource(R.string.confirm))
    }
  }
}