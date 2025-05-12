package ink.chyk.pass.viewmodels

import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.*
import ink.chyk.pass.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PassCodeViewModel(
  override val mmkv: MMKV,
  override val pass: PassApi = PassApi { false }
  // 需要使其抛出异常以便显示空白二维码
) : BasicViewModel(mmkv, pass) {
  // 通行码 ViewModel 
  // 具体实现：app 名称，以及几个方法

  override val appName: String = "passcode"

  override suspend fun newAppTicket(portalTicket: String): String {
    return pass.loginPassCodeTicket(portalTicket)
  }

  override suspend fun newAppSession(): CampusAppSession {
    return pass.newPassCodeSession()
  }

  override suspend fun loginApp(session: CampusAppSession, appTicket: String) {
    return pass.loginPassCode(session, appTicket)
  }

  // API
  private val passCodeApi = PassCode(pass)

  // 二维码数据
  private val _code = MutableStateFlow("")
  val code: StateFlow<String> = _code

  // 用户信息
  private val _userInfo = MutableStateFlow<PassCodeUserInfoResponse?>(null)
  val userInfo: StateFlow<PassCodeUserInfoResponse?> = _userInfo

  // 二维码生成时间
  private val _codeGenerateTime = MutableStateFlow(0L)
  val codeGenerateTime: StateFlow<Long> = _codeGenerateTime

  // 二维码过期时间
  private val _codeExpiredAt = MutableStateFlow(0L)
  // val codeExpiredAt: StateFlow<Long> = _codeExpiredAt
  // 在外部不使用，仅用于刷新


  /**
   * 获取二维码
   */
  private suspend fun realRefreshQRCode() {
    try {
      prepareSessionAnd { session ->
        // 重新获取二维码
        val qrCode = passCodeApi.getQRCodeString(session) ?: return@prepareSessionAnd
        if (_userInfo.value == null) {
          val codeInfo = pass.getPassCodeUserInfo(session) ?: return@prepareSessionAnd
          _userInfo.value = codeInfo.data[0].attributes
        }
        _code.value = qrCode.qrCodeString
        _codeGenerateTime.value = qrCode.createTime
        _codeExpiredAt.value = qrCode.expireTime
      }
    } catch (e: Exception) {
      // 网络问题
      Log.e("PassCodeViewModel", "Failed to refresh QR Code: ${e.message}")
      _code.value = ""
      _loadingState.value = LoadingState.FAILED
    }
  }

  fun refreshQRCode() {
    viewModelScope.launch {
      realRefreshQRCode()
    }
  }

  private fun startRefreshQRCode() {
    viewModelScope.launch {
      while (true) {
        // 计时器
        val currentTime: Long = System.currentTimeMillis()
        // Log.d("ECode", "Current Time: $currentTime, Expired At: $codeExpiredAt")
        if (currentTime >= _codeExpiredAt.value) {
          realRefreshQRCode()
        }

        // 每秒检查一次
        delay(1000L)
      }
    }
  }

  init {
    startRefreshQRCode()
  }

  fun isAntiFlashlight(): Boolean {
    return mmkv.decodeBool("anti_flashlight", false)
  }
}