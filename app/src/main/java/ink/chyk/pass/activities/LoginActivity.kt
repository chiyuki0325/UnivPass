package ink.chyk.pass.activities

import android.content.*
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tencent.mmkv.MMKV
import ink.chyk.pass.*
import kotlinx.coroutines.*
import ink.chyk.pass.R
import ink.chyk.pass.api.*
import ink.chyk.pass.ui.theme.*

class LoginActivity : ComponentActivity() {
  // 登录窗口
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val lightTransparentStyle = SystemBarStyle.light(
      scrim = TRANSPARENT,
      darkScrim = TRANSPARENT
    )
    enableEdgeToEdge(
      statusBarStyle = lightTransparentStyle,
      navigationBarStyle = lightTransparentStyle
    )

    val context = this

    setContent {
      AppTheme {
        Scaffold { innerPadding ->
          Box(
            modifier = Modifier
              .padding(innerPadding)
              .fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            LoginForm(openMainPage = {
              val intent = Intent(context, MainActivity::class.java)
              startActivity(intent)
              finish()
            })
          }
        }
      }
    }
  }
}

@Composable
fun LoginForm(openMainPage: () -> Unit = {}) {
  var studentId by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var showErrorMessage by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var studentIdFillsRequirement by remember { mutableStateOf(false) }
  val ctx = LocalContext.current

  Box(
    modifier = Modifier
      .fillMaxSize(),
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
        SchoolTitle(ctx.getString(R.string.uniform_auth))
        StudentIdInput(studentId, onStudentIdChange = {
          studentId = it
          studentIdFillsRequirement = it.length == 8
        })
        PasswordInput(
          password = password,
          onPasswordChange = { password = it },
          passwordVisible = passwordVisible,
          onPasswordVisibleChange = { passwordVisible = !passwordVisible }
        )
        LoginButton(
            studentId, password, openErrorDialog = { 
            // 错误处理：打开报错对话框，删除本地存储的密码
            password = ""
            val mmkv = MMKV.defaultMMKV()
            mmkv.remove("student_id")
            mmkv.remove("password")
            errorMessage = it
            showErrorMessage = true
            studentIdFillsRequirement = false
            licenseIdFillsRequirement = false
          },
          openMainPage = openMainPage,
          enabled = studentIdFillsRequirement, 
          onClickAnd = {
            studentIdFillsRequirement = false
            licenseIdFillsRequirement = false
          }
        )
        if (showErrorMessage) {
          ErrorDialog(errorMessage, onDismissRequest = {
            showErrorMessage = false
          })
        }
      }
    }
  }
}

@Composable
fun StudentIdInput(studentId: String, onStudentIdChange: (String) -> Unit) {
  // 学号输入框
  val ctx = LocalContext.current
  OutlinedTextField(
    value = studentId,
    onValueChange = onStudentIdChange,
    label = { Text(ctx.getString(R.string.student_id)) },
    leadingIcon = {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_person_24_filled),
        contentDescription = "Student ID"
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
  )
}

@Composable
fun PasswordInput(
  password: String,
  onPasswordChange: (String) -> Unit,
  passwordVisible: Boolean,
  onPasswordVisibleChange: () -> Unit
) {
  // 密码输入框
  val ctx = LocalContext.current

  OutlinedTextField(
    value = password,
    onValueChange = onPasswordChange,
    label = { Text(ctx.getString(R.string.password)) },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    visualTransformation = if (passwordVisible) {
      VisualTransformation.None
    } else {
      PasswordVisualTransformation()
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    leadingIcon = {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_password_24_filled),
        contentDescription = "Password"
      )
    },
    trailingIcon = {
      val icon = painterResource(
        if (passwordVisible) {
          R.drawable.ic_fluent_eye_24_filled
        } else {
          R.drawable.ic_fluent_eye_off_24_filled
        }
      )
      val description = ctx.getString(
        if (passwordVisible) {
          R.string.password_hide
        } else {
          R.string.password_show
        }
      )
      IconButton(onClick = onPasswordVisibleChange) {
        Icon(painter = icon, contentDescription = description)
      }
    },
  )
}


@Composable
fun LoginButton(
  studentId: String,
  password: String,
  openErrorDialog: (String) -> Unit,
  openMainPage: () -> Unit,
  enabled: Boolean,
  onClickAnd: () -> Unit
) {
  // 登录按钮
  val ctx = LocalContext.current
  val coroScope = rememberCoroutineScope()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 16.dp),
    horizontalArrangement = Arrangement.End
  ) {
    Button(
      onClick = {
        onClickAnd()
        val mmkv = MMKV.defaultMMKV()
        mmkv.encode("student_id", studentId)
        mmkv.encode("password", password)

        coroScope.launch {
          try {
            val pass = PassApi {
              throw RequestFailedException("请求失败")
            }
            val ticket = pass.loginPortalTicket(studentId, password)
            val personalTicket = pass.loginPersonalApiTicket(ticket)
            val personalSession = pass.loginPersonalApi(personalTicket)
            val studentInfo = pass.getUserInfo(personalSession)
            var licenseValid = true  // No OSS


            // 登录成功
            mmkv.encode("portal_ticket", ticket)
            openMainPage()
          } catch (_: PasswordIncorrectException) {
            // 密码错误
            openErrorDialog(ctx.getString(R.string.password_error_content))
          } catch (_: RequestFailedException) {
            // 请求失败
            openErrorDialog(ctx.getString(R.string.request_error_content))
          } catch (e: Exception) {
            // 其他错误
            openErrorDialog("$e ${e.message}")
          }
        }
      },
      enabled = enabled
    ) {
      Text(ctx.getString(R.string.login))
    }
  }
}

@Composable
fun ErrorDialog(errorMessage: String, onDismissRequest: () -> Unit = {}) {
  val ctx = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(ctx.getString(R.string.login_failed)) },
    text = { Text(errorMessage) },
    confirmButton = {
      Button(
        onClick = onDismissRequest
      ) {
        Text(ctx.getString(R.string.confirm))
      }
    }
  )
}