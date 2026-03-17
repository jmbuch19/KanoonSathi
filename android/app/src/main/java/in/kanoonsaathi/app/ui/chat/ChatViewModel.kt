package `in`.kanoonsaathi.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.kanoonsaathi.app.data.api.ChatMessageDto
import `in`.kanoonsaathi.app.data.repository.ChatRepository
import `in`.kanoonsaathi.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ChatUiState(
    val sessionId: String = "",
    val sessionTitle: String? = null,
    val isBookmarked: Boolean = false,
    val messages: List<ChatMessageDto> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val warning: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        _uiState.update { it.copy(sessionId = sessionId, isLoading = true) }
        viewModelScope.launch {
            when (val result = chatRepository.getSession(sessionId)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionTitle = result.data.title,
                        isBookmarked = result.data.isBookmarked,
                        messages = result.data.messages ?: emptyList(),
                    )
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(input = text) }

    fun sendMessage() {
        val content = _uiState.value.input.trim()
        if (content.isBlank() || _uiState.value.isSending) return

        val sessionId = _uiState.value.sessionId
        val tempUserMsg = ChatMessageDto(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = content,
            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
        )

        // Optimistic update — show message immediately before server responds
        _uiState.update {
            it.copy(
                messages = it.messages + tempUserMsg,
                input = "",
                isSending = true,
                warning = null,
            )
        }

        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(sessionId, content)) {
                is Result.Success -> {
                    val response = result.data
                    // Replace temp user message with real one, add AI response
                    _uiState.update {
                        it.copy(
                            messages = it.messages
                                .filter { m -> m.id != tempUserMsg.id }
                                + response.userMessage
                                + response.aiMessage,
                            isSending = false,
                            warning = response.warning,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            messages = it.messages.filter { m -> m.id != tempUserMsg.id },
                            isSending = false,
                            input = content, // restore input so user can retry
                            error = result.message,
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun sendPrompt(prompt: String) {
        _uiState.update { it.copy(input = prompt) }
        sendMessage()
    }

    fun toggleBookmark() {
        val sessionId = _uiState.value.sessionId
        viewModelScope.launch {
            when (val result = chatRepository.toggleBookmark(sessionId)) {
                is Result.Success -> _uiState.update { it.copy(isBookmarked = result.data) }
                else -> {}
            }
        }
    }

    fun reportMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.reportMessage(messageId, "User reported this message")
        }
    }

    fun clearWarning() = _uiState.update { it.copy(warning = null) }
}
