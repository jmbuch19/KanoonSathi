package `in`.kanoonsaathi.app.data.repository

import `in`.kanoonsaathi.app.data.api.ApiService
import `in`.kanoonsaathi.app.data.api.InternshipPostingDto
import `in`.kanoonsaathi.app.util.Result
import `in`.kanoonsaathi.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpportunitiesRepository @Inject constructor(private val api: ApiService) {

    suspend fun getOpportunities(): Result<List<InternshipPostingDto>> =
        safeApiCall {
            val res = api.getOpportunities()
            if (res.isSuccessful && res.body()?.success == true)
                res.body()!!.data ?: emptyList()
            else
                throw Exception(res.body()?.error?.message ?: "Failed to load opportunities")
        }

    suspend fun getUnreadCount(): Result<Int> =
        safeApiCall {
            val res = api.getUnreadOpportunityCount()
            if (res.isSuccessful && res.body()?.success == true)
                res.body()!!.data!!.count
            else
                0
        }
}
