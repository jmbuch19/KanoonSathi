package `in`.kanoonsaathi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.kanoonsaathi.app.data.repository.AuthRepository
import `in`.kanoonsaathi.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OtpNavigation {
    object Dashboard     : OtpNavigation()   // existing user
    object Onboarding    : OtpNavigation()   // new user — role auto-set
    data class RoleSelect(val userId: String) : OtpNavigation()  // new user — no pre-selected role
}

data class OtpUiState(
    val email: String = "",
    val role: String = "NONE",
    val otp: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val resendCooldown: Int = 60,
    val navigation: OtpNavigation? = null,
)

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()
    private var cooldownJob: Job? = null

    fun setEmailAndRole(email: String, role: String) {
        _uiState.update { it.copy(email = email, role = role) }
        startCooldown()
    }

    fun onOtpChange(otp: String) {
        _uiState.update { it.copy(otp = otp, isError = false) }
        if (otp.length == 6) verifyOtp()
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.otp.length != 6 || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            when (val result = authRepository.verifyOtp(state.email, state.otp)) {
                is Result.Success -> {
                    val data = result.data
                    if (!data.isNewUser) {
                        // Existing user — tokens already saved in repo
                        _uiState.update { it.copy(isLoading = false, navigation = OtpNavigation.Dashboard) }
                    } else {
                        val role = state.role
                        // New user with pre-selected role → auto-set role, skip role select screen
                        if (data.setupToken != null && role != "NONE") {
                            when (authRepository.setRole(role)) {
                                is Result.Success -> _uiState.update {
                                    it.copy(isLoading = false, navigation = OtpNavigation.Onboarding)
                                }
                                is Result.Error -> _uiState.update {
                                    it.copy(isLoading = false, navigation = OtpNavigation.RoleSelect(""))
                                }
                                else -> {}
                            }
                        } else if (data.setupToken != null) {
                            // New user, no pre-selected role → go to role select
                            _uiState.update { it.copy(isLoading = false, navigation = OtpNavigation.RoleSelect("")) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, isError = true) }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, isError = true, otp = "") }
                }
                else -> {}
            }
        }
    }

    fun resendOtp() {
        val email = _uiState.value.email
        viewModelScope.launch {
            authRepository.sendOtp(email)
            _uiState.update { it.copy(otp = "", isError = false) }
            startCooldown()
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in 60 downTo 0) {
                _uiState.update { it.copy(resendCooldown = i) }
                if (i > 0) delay(1000)
            }
        }
    }

    fun clearNavigation() = _uiState.update { it.copy(navigation = null) }
}
