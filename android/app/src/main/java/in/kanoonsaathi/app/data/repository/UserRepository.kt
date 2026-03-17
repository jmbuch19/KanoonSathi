package `in`.kanoonsaathi.app.data.repository

import `in`.kanoonsaathi.app.data.api.*
import `in`.kanoonsaathi.app.util.Result
import `in`.kanoonsaathi.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val api: ApiService) {

    suspend fun submitStudentOnboarding(request: StudentOnboardingRequest): Result<Unit> = safeApiCall {
        val res = api.submitStudentOnboarding(request)
        if (res.isSuccessful && res.body()?.success == true) Unit
        else throw Exception(res.body()?.error?.message ?: "Onboarding failed")
    }

    suspend fun submitFacultyOnboarding(request: FacultyOnboardingRequest): Result<Unit> = safeApiCall {
        val res = api.submitFacultyOnboarding(request)
        if (res.isSuccessful && res.body()?.success == true) Unit
        else throw Exception(res.body()?.error?.message ?: "Onboarding failed")
    }

    suspend fun submitCuriousOnboarding(request: CuriousOnboardingRequest): Result<Unit> = safeApiCall {
        val res = api.submitCuriousOnboarding(request)
        if (res.isSuccessful && res.body()?.success == true) Unit
        else throw Exception(res.body()?.error?.message ?: "Onboarding failed")
    }

    suspend fun getUsage(): Result<UsageResponse> = safeApiCall {
        val res = api.getUsage()
        if (res.isSuccessful && res.body()?.success == true) res.body()!!.data!!
        else throw Exception(res.body()?.error?.message ?: "Failed to fetch usage")
    }
}
