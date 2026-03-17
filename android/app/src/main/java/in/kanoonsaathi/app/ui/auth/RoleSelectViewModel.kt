package `in`.kanoonsaathi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.kanoonsaathi.app.data.repository.AuthRepository
import `in`.kanoonsaathi.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleSelectUiState(
    val selectedRole: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToOnboarding: Boolean = false,
)

@HiltViewModel
class RoleSelectViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectUiState())
    val uiState: StateFlow<RoleSelectUiState> = _uiState.asStateFlow()

    // userId param kept for source-compatibility with RoleSelectScreen.
    // The actual userId is carried by the setup token in TokenStore — no need to pass it here.
    fun selectRole(userId: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedRole = role, isLoading = true, error = null) }
            when (val result = authRepository.setRole(role)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, navigateToOnboarding = true) }
                is Result.Error  -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun clearNavigation() = _uiState.update { it.copy(navigateToOnboarding = false) }
}
