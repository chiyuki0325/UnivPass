package ink.chyk.pass.screens

import android.annotation.*
import android.content.*
import android.util.*
import android.widget.Toast
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.navigation.*
import ink.chyk.pass.*
import ink.chyk.pass.activities.*
import ink.chyk.pass.api.*
import ink.chyk.pass.viewmodels.*
import ink.chyk.pass.R
import coil3.compose.AsyncImage
import coil3.network.*
import coil3.request.*

@SuppressLint("DefaultLocale")
@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  navController: NavController,
  innerPadding: PaddingValues
) {
  val userInfo by viewModel.userInfo.collectAsState()
  val cardBalance by viewModel.cardBalance.collectAsState()
  val netBalance by viewModel.netBalance.collectAsState()
  val headers by viewModel.headers.collectAsState()
  val loadComplete by viewModel.loadComplete.collectAsState()

  val showLogoutDialog = remember { mutableStateOf(false) }
  val showAboutDialog = remember { mutableStateOf(false) }
  val showUploadAvatarDialog = remember { mutableStateOf(false) }
  val byteArray = remember { mutableStateOf<ByteArray?>(null) }
  val mimeType = remember { mutableStateOf<String?>(null) }
  val fileName = remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    viewModel.refreshUserInfo()
  }


  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(vertical = 64.dp, horizontal = 8.dp)
  ) {
    Column {
      ProfileHeader(
        userInfo, headers, loadComplete,
        onUploadImage = { _byteArray: ByteArray, _mimeType: String, _fileName: String ->
          showUploadAvatarDialog.value = true
          byteArray.value = _byteArray
          mimeType.value = _mimeType
          fileName.value = _fileName
        }
      )
      Spacer(modifier = Modifier.height(64.dp))
      RowButton(
        iconResource = R.drawable.ic_fluent_contact_card_24_regular,
        text = stringResource(R.string.campus_card),
        content = if (loadComplete) "${stringResource(R.string.campus_card_balance)} ${cardBalance?.valueString} ${cardBalance?.unit}" else null,
        clickable = false
      )
      val netBalanceFloat = netBalance?.valueString?.toFloatOrNull()

      RowButton(
        iconResource = R.drawable.ic_fluent_desktop_signal_24_regular,
        text = stringResource(R.string.network),
        content = if (loadComplete) "${stringResource(R.string.network_balance)} ${
          netBalanceFloat?.let {
            // 保留两位小数
            if (it < 0) stringResource(R.string.lack_balance) else String.format("%.2f", it)
          }
        } ${netBalance?.unit}" else null,
        clickable = false
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_payment_24_regular,
        text = stringResource(R.string.recharge),
        clickable = true,
        onClick = { viewModel.recharge(context) }
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_person_swap_24_regular,
        text = stringResource(R.string.logout_account),
        clickable = true,
        onClick = { showLogoutDialog.value = true }
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_settings_24_regular,
        text = stringResource(R.string.settings),
        clickable = true,
        onClick = {
          // 跳转到设置页面
          val intent = Intent(context, SettingsActivity::class.java)
          context.startActivity(intent)
        }
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_book_information_24_regular,
        text = stringResource(R.string.about),
        clickable = true,
        onClick = { showAboutDialog.value = true }
      )
    }
  }

  LogoutConfirmationDialog(
    showDialog = showLogoutDialog,
    onConfirm = { viewModel.logout(context) },
    onDismiss = { showLogoutDialog.value = false }
  )

  AboutDialog(
    showDialog = showAboutDialog,
    onDismiss = { showAboutDialog.value = false },
  )

  UploadAvatarConfirmDialog(
    showDialog = showUploadAvatarDialog,
    onDismiss = { showUploadAvatarDialog.value = false },
    onConfirm = {
      Toast.makeText(context, context.getString(R.string.uploading), Toast.LENGTH_SHORT).show()
      showUploadAvatarDialog.value = false
      try {
        viewModel.uploadAvatar(byteArray.value!!, mimeType.value!!, fileName.value!!, {
          Toast.makeText(context, context.getString(R.string.upload_finished), Toast.LENGTH_LONG)
            .show()
        })
      } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.upload_failed), Toast.LENGTH_SHORT).show()
      }
    }
  )
}

@Composable
fun ProfileHeader(
  userInfo: UserInfo?,
  headers: NetworkHeaders?,
  loadComplete: Boolean,
  onUploadImage: (ByteArray, String, String) -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val resolver = context.contentResolver
  // 上传头像选择器
  val pickMedia =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        Log.d("PhotoPicker", "Selected URI: $uri")
        // 上传头像
        resolver.openInputStream(uri).use { stream ->
          val byteArray = stream?.readBytes()
          if (byteArray != null) {
            val mimeType = resolver.getType(uri)!!
            // 生成文件名
            val fileName =
              "avatar${uri.pathSegments.last()}.${mimeType.substringAfter("/")}".replace(
                "jpeg",
                "jpg"
              )
            onUploadImage(byteArray, mimeType, fileName)
          } else {
            Toast.makeText(
              context,
              context.getString(R.string.cannot_read_selected_image),
              Toast.LENGTH_SHORT
            ).show()
          }
        }
      }
    }

  // 大头，学号等

  var height by remember { mutableIntStateOf(0) }
  val heightDp = with(LocalDensity.current) { height.toDp() }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .onSizeChanged {
        height = it.height
      }
  ) {
    if (loadComplete) {
      var avatarUrl = userInfo?.avatar
      // 头像
      if (avatarUrl?.startsWith("/") == true) {
        avatarUrl = "http://localhost:8889" + avatarUrl // 生产环境记得替换！ 
      }
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(avatarUrl)
          .crossfade(true)
          .apply {
            if (headers != null) {
              httpHeaders(headers)
            }
          }
          .build(),
        contentDescription = "Avatar",
        modifier = Modifier
          .height(heightDp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
      )
      Spacer(modifier = Modifier.width(16.dp))
      // 文字
      Column {
        Text(
          text = userInfo?.name ?: "",
          style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "${userInfo?.depart} ${userInfo?.identity}\n学号: ${userInfo?.xgh}\n邮箱: ${userInfo?.email}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.clickable {
            clipboardManager.setText(buildAnnotatedString { userInfo?.xgh })
            Toast.makeText(context, "已复制学号", Toast.LENGTH_SHORT).show()
          },
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
    } else {
      Box(modifier = Modifier.size(96.dp)) { CircularProgressIndicator() }
    }
  }
}


@Composable
fun RowButton(
  iconResource: Int,
  text: String,
  content: String? = null,
  clickable: Boolean = false,
  onClick: () -> Unit = {}
) {
  var rowModifier = Modifier
    .fillMaxWidth()
    .height(48.dp)
    .padding(horizontal = 16.dp)
    .clip(RoundedCornerShape(8.dp))
  if (clickable) {
    rowModifier = rowModifier.clickable(onClick = onClick)
  }
  Row(
    modifier = rowModifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row {
      Icon(
        painter = painterResource(iconResource),
        contentDescription = text
      )
      Spacer(modifier = Modifier.width(16.dp))
      Text(text)
      if (content != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(content, color = MaterialTheme.colorScheme.secondary)
      }
    }
    if (clickable) {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_chevron_right_20_filled),
        contentDescription = "前往"
      )
    } else {
      Box {}
    }
  }
}

@Composable
fun LogoutConfirmationDialog(
  showDialog: MutableState<Boolean>,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = {
        // 当点击外部区域或返回键时关闭对话框
        onDismiss()
      },
      title = {
        DialogTitle(R.drawable.ic_fluent_person_swap_24_regular, "登出")
      },
      text = {
        Text("确定要登出当前的账号吗？")
      },
      confirmButton = {
        Button(onClick = {
          onConfirm()
          showDialog.value = false // 关闭对话框
        }) {
          Text("确认")
        }
      },
      dismissButton = {
        Button(onClick = {
          onDismiss()
          showDialog.value = false // 关闭对话框
        }) {
          Text("取消")
        }
      }
    )
  }
}


@Composable
fun AboutDialog(
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
) {
  if (showDialog.value) {
    val packageInfo =
      LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0)
    val versionName = packageInfo.versionName ?: "Development Build"

    Dialog(
      onDismissRequest = onDismiss,
    ) {
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
            R.drawable.ic_fluent_book_information_24_regular,
            stringResource(R.string.about)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.about_content_1),
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.about_version, versionName),
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}

@Composable
fun UploadAvatarConfirmDialog(
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        DialogTitle(
          R.drawable.ic_fluent_person_swap_24_regular,
          stringResource(R.string.upload_avatar)
        )
      },
      text = {
        Text(stringResource(R.string.upload_avatar_confirm))
      },
      confirmButton = {
        Button(onClick = {
          onConfirm()
          showDialog.value = false // 关闭对话框
        }) {
          Text(stringResource(R.string.confirm))
        }
      },
      dismissButton = {
        Button(onClick = {
          onDismiss()
          showDialog.value = false // 关闭对话框
        }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}