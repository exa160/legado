package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import retrofit2.Response

object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        coroutineScope: CoroutineScope,
        response: Response<String>,
        book: Book,
        bookChapter: BookChapter,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl
    ): String {
        val baseUrl: String = NetworkUtils.getUrl(response)
        val body: String? = response.body()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.get_web_content_error,
                baseUrl
            )
        )
        val content = StringBuilder()
        val nextUrlList = arrayListOf(baseUrl)
        val contentRule = bookSource.getContentRule()
        var contentData = analyzeContent(body, contentRule, book, baseUrl)
        content.append(contentData.content)
        if (contentData.nextUrl.size == 1) {
            var nextUrl = contentData.nextUrl[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)
                AnalyzeUrl(ruleUrl = nextUrl, book = book).getResponse().execute()
                    .body()?.let { nextBody ->
                        contentData = analyzeContent(nextBody, contentRule, book, baseUrl)
                        nextUrl = if (contentData.nextUrl.isNotEmpty()) contentData.nextUrl[0] else ""
                        content.append(contentData.content)
                    }
            }
        } else if (contentData.nextUrl.size > 1) {

        }
        return content.toString()
    }

    private fun analyzeContent(
        body: String,
        contentRule: ContentRule,
        book: Book,
        baseUrl: String
    ): ContentData<List<String>> {
        val nextUrlList = arrayListOf<String>()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.getStringList(contentRule.nextContentUrl ?: "", true)?.let {
            nextUrlList.addAll(it)
        }
        return ContentData("", nextUrlList)
    }
}