package `in`.kanoonsaathi.app.ui.bookmarks

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

data class BookmarksUiState(
    val sessions: List<ChatSessionDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState = _uiState.asStateFlow()

    init { loadBookmarks() }

    fun loadBookmarks() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = chatRepo.getBookmarks()) {
                is Result.Success -> _uiState.update { it.copy(sessions = result.data, isLoading = false) }
                is Result.Error   -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                else -> {}
            }
        }
    }
}
