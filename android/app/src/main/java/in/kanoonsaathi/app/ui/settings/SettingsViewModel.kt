package `in`.kanoonsaathi.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.kanoonsaathi.app.data.api.UserDto
import `in`.kanoonsaathi.app.data.api.UsageResponse
import `in`.kanoonsaathi.app.data.repository.AuthRepository
import `in`.kanoonsaathi.app.data.repository.UserRepository
import `in`.kanoonsaathi.app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: UserDto? = null,
    val usage: UsageResponse? = null,
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val loggedOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            // Fetch both in parallel conceptually (sequential here is fine — no perf concern)
            val userResult = authRepo.getMe()
            val usageResult = userRepo.getUsage()
            _uiState.update { state ->
                state.copy(
                    user = (userResult as? Result.Success)?.data,
                    usage = (usageResult as? Result.Success)?.data,
                    isLoading = false,
                    error = (userResult as? Result.Error)?.message,
                )
            }
        }
    }

    fun logout() {
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            authRepo.logout()
            _uiState.update { it.copy(isLoggingOut = false, loggedOut = true) }
        }
    }

    fun deleteAccount() {
        _uiState.update { it.copy(isDeletingAccount = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepo.deleteAccount()) {
                is Result.Success -> _uiState.update {
                    it.copy(isDeletingAccount = false, accountDeleted = true)
                }
                is Result.Error   -> _uiState.update {
                    it.copy(isDeletingAccount = false, error = result.message)
                }
                else -> {}
            }
        }
    }
}
