package ink.chyk.pass.viewmodels

import android.content.Context
import android.content.Intent
import android.util.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.*
import coil3.network.*
import com.tencent.mmkv.*
import ink.chyk.pass.*
import ink.chyk.pass.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ink.chyk.pass.activities.*
import ink.chyk.pass.api.*

class ProfileViewModel(
  val mmkv: MMKV,
  val pass: PassAPI
) : PersonalViewModel(pass, mmkv) {
  private val _userInfo = MutableStateFlow<UserInfo?>(null)
  val userInfo: StateFlow<UserInfo?> = _userInfo
  private val _cardBalance = MutableStateFlow<PersonalDataItem?>(null)
  val cardBalance: StateFlow<PersonalDataItem?> = _cardBalance
  private val _netBalance = MutableStateFlow<PersonalDataItem?>(null)
  val netBalance: StateFlow<PersonalDataItem?> = _netBalance
  private val _loadComplete = MutableStateFlow(false)
  private val _headers = MutableStateFlow<NetworkHeaders?>(null)
  val headers: StateFlow<NetworkHeaders?> = _headers
  val loadComplete: StateFlow<Boolean> = _loadComplete


  fun refreshUserInfo() {
    viewModelScope.launch {
      _refreshUserInfo()
    }
  }

  private suspend fun _refreshUserInfo() {
    prepareSessionAnd { session ->
      try {
        // 2025.5.1 使用协程来并行处理部分网络请求，以加快 ui 的加载速度
        // session 的先后，已经无所谓啦！（开摆！

        val ids: PersonalDataIdOuter?

        // 第一步：获取用户信息
        val result = coroutineScope {
          val deferredUserInfo = async { pass.getUserInfo(session) }
          val deferredIds = async { pass.getPersonalDataIds(session) }
          val (userInfoResponse, idsResponse) = awaitAll(deferredUserInfo, deferredIds)
          _userInfo.value = (userInfoResponse.first as UserInfoOuter?)?.info
          ids = idsResponse.first as PersonalDataIdOuter?
          // updateSession(userInfoResponse.second)

          return@coroutineScope ids != null
        }

        if (!result) {
          // ids 获取失败
          return@prepareSessionAnd
        }


        // 第二步：获取校园卡余额、网络余额
        coroutineScope {
          @Suppress("NAME_SHADOWING")
          val ids = ids!!
          val deferredCardBalance = async { pass.getCardBalance(session) } 
          val deferredNetBalance = async { pass.getNetBalance(session) } 
          val (cardBalanceResponse, netBalanceResponse) = awaitAll(deferredCardBalance, deferredNetBalance)
          _cardBalance.value = cardBalanceResponse.first?.data
          _netBalance.value = netBalanceResponse.first?.data
        }

        _headers.value = NetworkHeaders.Builder()
          .add("Referer", "http://localhost:8070") 
          .build()

        _loadComplete.value = true
      } catch (_: SessionExpiredException) {
        // 重新登录
        _refreshUserInfo()
      }
    }
  }

  fun logout(context: Context) {
    mmkv.clearAll()
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
  }

  private suspend fun _recharge(context: Context) = withContext(Dispatchers.IO) {
    var portalTicket = getPortalTicket()
    var rechargeTicket: String
    val url = "http://localhost:8080"  // 生产环境中替换为充值跳转网址 

    try {
      rechargeTicket = pass.loginCampusAppTicket(portalTicket, url)
    } catch (e: TicketFailedException) {
      portalTicket = getPortalTicket(true)
      rechargeTicket = pass.loginCampusAppTicket(portalTicket, url)
    }

    val redirectUrl = "$url" // 根据后端不同，修改 rechargeTicket 参数名 

    // Custom Tabs
    val intent = CustomTabsIntent.Builder()
      .setShowTitle(false)
      .setUrlBarHidingEnabled(true)
      .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
      .setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
      .build()

    intent.launchUrl(context, android.net.Uri.parse(redirectUrl))
  }

  fun recharge(context: Context) {
    viewModelScope.launch {
      _recharge(context)
    }
  }

  fun uploadAvatar(
    byteArray: ByteArray,
    mimeType: String,
    fileName: String,
    onUploadComplete: () -> Unit
  ) {
    Log.d("uploadAvatar", "Uploading avatar file $fileName")
    viewModelScope.launch {
      prepareSessionAnd { session ->
        val response = pass.uploadImage(session, byteArray, mimeType, fileName)
        if (response.first == null) {
          Log.d("uploadAvatar", "Upload failed")
          throw Exception("Upload failed")
        }
        Log.d("uploadAvatar", "Response: ${response.first}")
        Log.d("uploadAvatar", "Uploaded url: ${response.first!!.url}")
        _userInfo.value = _userInfo.value?.copy(avatar = response.first!!.url)
        val response2 = pass.updateAvatar(session, response.first!!.url)
        Log.d("uploadAvatar", "Update avatar response: ${response2.first}")
        onUploadComplete()
      }
    }
  }


}