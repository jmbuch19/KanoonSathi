package `in`.kanoonsaathi.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.kanoonsaathi.app.data.api.ChatSessionDto
import `in`.kanoonsaathi.app.data.repository.ChatRepository
import `in`.kanoonsaathi.app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatHistoryUiState(
    val sessions: List<ChatSessionDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deletingId: String? = null,
)

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init { loadSessions() }

    fun loadSessions() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = chatRepo.getSessions(1, 50)) {
                is Result.Success -> _uiState.update { it.copy(sessions = result.data.sessions, isLoading = false) }
                is Result.Error   -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                else -> {}
            }
        }
    }

    fun deleteSession(sessionId: String) {
        _uiState.update { it.copy(deletingId = sessionId) }
        viewModelScope.launch {
            when (chatRepo.deleteSession(sessionId)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.filter { it.id != sessionId },
                        deletingId = null,
                    )
                }
                is Result.Error -> _uiState.update { it.copy(deletingId = null) }
                else -> {}
            }
        }
    }
}
