package ink.chyk.pass

import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class HitokotoQuote(
  val id: Int,
  val uuid: String,
  val hitokoto: String,
  val type: String,
  val from: String,
  @SerialName("from_who") val fromWho: String?,
  val creator: String,
  @SerialName("creator_uid") val creatorUid: Int,
  val reviewer: Int,
  @SerialName("commit_from") val commitFrom: String,
  @SerialName("created_at") val createdAt: Long,
  val length: Int
)

class Hitokoto {
  // 通过一言接口获取每日一言
  // https://hitokoto.cn/
  // https://developer.hitokoto.cn/sentence/

  suspend fun getQuote(): HitokotoQuote {
    // Create OkHttp client
    val client = OkHttpClient()

    // Create request object
    val request = Request.Builder()
      .url("https://v1.hitokoto.cn")
      .build()

    // Make the request
    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Unexpected code $response")

        // Parse JSON response
        val jsonResponse = response.body?.string()
        if (jsonResponse != null) {
          // Deserialize the JSON string to HitokotoResponse
          val hitokotoResponse = Json.decodeFromString<HitokotoQuote>(jsonResponse)
          hitokotoResponse
        } else {
          throw Exception("No response body")
        }
      }
    }
  }
}