package ink.chyk.pass.viewmodels

import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.*
import ink.chyk.pass.api.*
import kotlinx.coroutines.flow.*

abstract class BasicViewModel(
  open val mmkv: MMKV,
  open val pass: PassApi
) : ViewModel() {
  // 一些参数，Kotlin 状态流机制

  // ChatGPT: 带下划线的是私有变量，Mutable 意为可以在这个 ViewModel 内部修改
  // 而不带下划线的是公开的，并且它是 StateFlow，只读，只能接收不能修改

  // 是否加载完毕
  internal val _loadingState = MutableStateFlow(LoadingState.LOADING)
  val loadingState: StateFlow<LoadingState> = _loadingState


  // 下面这些都是用来 override 的
  abstract val appName: String  // 在 mmkv 中储存的小程序名称

  abstract suspend fun newAppTicket(portalTicket: String): String

  abstract suspend fun newAppSession(): CampusAppSession

  abstract suspend fun loginApp(session: CampusAppSession, appTicket: String)

  private suspend fun getPortalTicket(reLogin: Boolean = false): String {
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!

    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = pass.loginPortalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }

    return portalTicket
  }

  /**
   * 通过学校门户 ticket 获取 app ticket
   * 如果 mmkv 中没有记录，则重新登录。
   *
   * @return ticket
   */
  private suspend fun getAppTicket(): String {
    var appTicket: String? = mmkv.decodeString("${appName}_ticket")
    if (appTicket == null) {
      var portalTicket = getPortalTicket()
      try {
        // 假设 portalTicket 没过期
        appTicket = newAppTicket(portalTicket)
      } catch (_: TicketFailedException) {
        // 重新登录
        portalTicket = getPortalTicket(true)
        appTicket = newAppTicket(portalTicket)
      }
      mmkv.encode("${appName}_ticket", appTicket)
    }
    return appTicket!! // 不可能为空!!
  }

  /**
   * 获取或新建 Session
   * 如果 mmkv 中没有记录，则新建空 Session。
   *
   * @return Session
   */
  private suspend fun getAppSession(mmkv: MMKV): CampusAppSession {
    var appSession: String? = mmkv.decodeString("${appName}_session")
    var expiredAt: Long? = mmkv.decodeLong("${appName}_expired_at")  // 以秒为单位

    val current = System.currentTimeMillis() / 1000
    if (appSession == null || xsrfToken == null || expiredAt == null || expiredAt < current) {
      Log.d("BasicViewModel", "Session 过期")
      val session = pass.newPassCodeSession()
      appSession = session.session
      expiredAt = session.expiredAt
      mmkv.encode("${appName}_session", appSession)
      mmkv.encode("${appName}_expired_at", expiredAt)
    }

    // 不可能为空!!
    return CampusAppSession(appSession, expiredAt) 
  }


  suspend fun <T> prepareSessionAnd(action: suspend (CampusAppSession) -> T) {
    try {
      val session = getAppSession(mmkv)
      try {
        action(session)
      } catch (_: SessionExpiredException) {
        // 重新准备 session
        val newSession = newAppSession()
        mmkv.encode("${appName}_session", newSession.session)
        mmkv.encode("${appName}_expired_at", newSession.expiredAt)

        // 重新登录
        val appTicket = getAppTicket()

        try {
          loginApp(newSession, appTicket)
        } catch (_: TicketExpiredException) {
          // 重新获取 ticket
          val portalTicket = getPortalTicket(true)
          val newAppTicket = newAppTicket(portalTicket)
          mmkv.encode("${appName}_ticket", newAppTicket)
          loginApp(newSession, newAppTicket)
        }
        action(newSession)
      }
      _loadingState.value = LoadingState.SUCCESS
    } catch (e: Exception) {
      // 错误处理逻辑
      Log.e("BasicViewModel", "Error: ${e.message}")
      e.printStackTrace()
      // throw e
      _loadingState.value = LoadingState.FAILED
    }
  }

}