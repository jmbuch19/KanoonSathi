package `in`.kanoonsaathi.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.kanoonsaathi.app.data.api.ChatModeDto
import `in`.kanoonsaathi.app.data.api.ChatSessionDto
import `in`.kanoonsaathi.app.data.api.InternshipPostingDto
import `in`.kanoonsaathi.app.data.api.NewsItemDto
import `in`.kanoonsaathi.app.data.repository.AuthRepository
import `in`.kanoonsaathi.app.data.repository.ChatRepository
import `in`.kanoonsaathi.app.data.repository.NewsRepository
import `in`.kanoonsaathi.app.data.repository.OpportunitiesRepository
import `in`.kanoonsaathi.app.security.TokenStore
import `in`.kanoonsaathi.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val greeting: String = "Hello!",
    val roleLabel: String = "",
    val isLoadingModes: Boolean = true,
    val modes: List<ChatModeDto> = emptyList(),
    val suggestedPrompts: Map<String, List<String>> = emptyMap(),
    val recentSessions: List<ChatSessionDto> = emptyList(),
    val todayMessages: Int = 0,
    val newsFeed: List<NewsItemDto> = emptyList(),
    val isLoadingNews: Boolean = true,
    val opportunities: List<InternshipPostingDto> = emptyList(),
    val isLoadingOpportunities: Boolean = true,
    val unreadOpportunities: Int = 0,
) {
    // no error state on dashboard — fail silently, show empty states
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val newsRepository: NewsRepository,
    private val opportunitiesRepository: OpportunitiesRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val role = tokenStore.getRole() ?: "STUDENT"
        _uiState.update {
            it.copy(
                roleLabel = when (role) {
                    "STUDENT" -> "Law Student"
                    "FACULTY" -> "Law Faculty"
                    "CURIOUS" -> "Curious About Law"
                    else -> ""
                },
                greeting = "Good ${timeOfDay()}!",
            )
        }
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            launch {
                when (val result = chatRepository.getModes()) {
                    is Result.Success -> _uiState.update { it.copy(isLoadingModes = false, modes = result.data) }
                    else -> _uiState.update { it.copy(isLoadingModes = false) }
                }
            }
            launch {
                when (val result = chatRepository.getSuggestedPrompts()) {
                    is Result.Success -> _uiState.update { it.copy(suggestedPrompts = result.data) }
                    else -> {}
                }
            }
            launch {
                when (val result = chatRepository.getSessions()) {
                    is Result.Success -> _uiState.update { it.copy(recentSessions = result.data.sessions) }
                    else -> {}
                }
            }
            launch {
                when (val result = newsRepository.getNewsFeed(limit = 15)) {
                    is Result.Success -> _uiState.update { it.copy(newsFeed = result.data, isLoadingNews = false) }
                    else -> _uiState.update { it.copy(isLoadingNews = false) }
                }
            }
            launch {
                val unread = when (val result = opportunitiesRepository.getUnreadCount()) {
                    is Result.Success -> result.data
                    else -> 0
                }
                when (val result = opportunitiesRepository.getOpportunities()) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            opportunities = result.data,
                            isLoadingOpportunities = false,
                            unreadOpportunities = unread,
                        )
                    }
                    else -> _uiState.update { it.copy(isLoadingOpportunities = false) }
                }
            }
        }
    }

    private fun timeOfDay(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            else -> "Evening"
        }
    }
}
