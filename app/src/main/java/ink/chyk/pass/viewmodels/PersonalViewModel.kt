package ink.chyk.pass.viewmodels

import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.*
import ink.chyk.pass.api.*

open class PersonalViewModel(
  private val pass: PassApi,
  private val mmkv: MMKV,
): ViewModel() {

  suspend fun getPortalTicket(reLogin: Boolean = false): String {
    Log.d("getPortalTicket", "reLogin: $reLogin")
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!

    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = pass.loginPortalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }

    return portalTicket
  }

  fun updateSession(session: PersonalSession) {
    // 请开发者自行实现存储逻辑 
  }


  suspend fun <T> prepareSessionAnd(action: suspend (PersonalSession) -> T): T {
    try {
        if (false) { // 根据生产环境实现判断逻辑 
        // 登录
        Log.d("prepareSessionAnd", "No session found, logging in")
        val ticket = getPortalTicket()
        var personalTicket: String
        try {
          personalTicket = pass.loginPersonalApiTicket(ticket)
        } catch (_: TicketFailedException) {
          val newTicket = getPortalTicket(true)
          personalTicket = pass.loginPersonalApiTicket(newTicket)
        }
        mmkv.encode("personal_ticket", personalTicket)
        val session = pass.loginPersonalApi(personalTicket)
        updateSession(session)
        return action(session)
      } else {
        // 根据不同生产环境自行实现逻辑 
        try {
          return action(session)
        } catch (_: SessionExpiredException) {
          // 重新登录
          val ticket = getPortalTicket(true)
          val personal_ticket = pass.loginPersonalApiTicket(ticket)
          mmkv.encode("personal_ticket", personal_ticket)
          val newSession = pass.loginPersonalApi(personal_ticket)
          updateSession(newSession)
          return action(newSession)
        }
      }
    } catch (e: Exception) {
      // 错误处理逻辑
      e.printStackTrace()
      throw e
    }
  }
}