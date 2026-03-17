package `in`.kanoonsaathi.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.kanoonsaathi.app.data.repository.ChatRepository
import `in`.kanoonsaathi.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatCreateViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    fun createSession(modeId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = chatRepository.createSession(modeId)) {
                is Result.Success -> onCreated(result.data.id)
                is Result.Error -> { /* TODO: show error, navigate back */ }
                else -> {}
            }
        }
    }
}
