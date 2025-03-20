package ink.chyk.pass.api

interface IPassAPI {
  // Universal Pass 前端开源代码附带此接口
  // 供使用本前端项目的开发者自行实现

  // 请求 Pass API 时所用的 User Agent
  var userAgent: String?

  // 使用学号与密码登录校园门户 API
  // 返回校园门户登录凭证
  suspend fun loginPortalTicket(studentId: String, password: String): String

  // 使用校园门户凭证登录学校内部 API
  // 如图书馆系统，综合教务系统等
  suspend fun loginCampusAppTicket(portalTicket: String, callbackUrl: String): String
}