package ink.chyk.pass

import kotlin.Pair
import android.content.Context
import kotlinx.serialization.json.Json


class Hitokoto {
  // 通过本地储存的 hitokoto_sentences.json 获取每日一言
  companion object {
    private var isInit = false
    private lateinit var quotes: List<Pair<String, String>>

    private fun initQuotes(ctx: Context) {
      val quoteText = ctx.resources.openRawResource(R.raw.hitokoto_sentences)
        .bufferedReader()
        .use { it.readText() }
      Json.decodeFromString<List<List<String>>>(quoteText).let { list ->
        quotes = list.map {
          Pair(it[0], it[1])
        }
      }
      isInit = true
    }

    fun getQuote(
      ctx: Context
    ): Pair<String, String> {
      if (!isInit) {
        initQuotes(ctx)
      }
      val index = (System.currentTimeMillis() % quotes.size).toInt()
      return quotes[index]
    }
  }
}