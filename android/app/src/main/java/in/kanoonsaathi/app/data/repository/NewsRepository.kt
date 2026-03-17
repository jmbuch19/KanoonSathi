package `in`.kanoonsaathi.app.data.repository

import `in`.kanoonsaathi.app.data.api.ApiService
import `in`.kanoonsaathi.app.data.api.NewsItemDto
import `in`.kanoonsaathi.app.util.Result
import `in`.kanoonsaathi.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(private val api: ApiService) {

    suspend fun getNewsFeed(limit: Int = 20, category: String? = null): Result<List<NewsItemDto>> =
        safeApiCall {
            val res = api.getNewsFeed(limit = limit, category = category)
            if (res.isSuccessful && res.body()?.success == true)
                res.body()!!.data!!.items
            else
                throw Exception(res.body()?.error?.message ?: "Failed to load news")
        }
}
