package `in`.kanoonsaathi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.kanoonsaathi.app.data.repository.AuthRepository
import `in`.kanoonsaathi.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isEmailValid: Boolean
        get() = email.contains("@") && email.contains(".") && email.length > 5
    val emailError: String?
        get() = if (email.isNotEmpty() && !isEmailValid) "Enter a valid email address" else null
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email.trim().lowercase(), error = null) }
    }

    fun sendOtp(onSuccess: (String) -> Unit) {
        val email = _uiState.value.email
        if (!_uiState.value.isEmailValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.sendOtp(email)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(email)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
