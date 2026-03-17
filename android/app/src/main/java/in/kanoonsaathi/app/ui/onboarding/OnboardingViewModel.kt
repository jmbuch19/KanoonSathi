package `in`.kanoonsaathi.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.kanoonsaathi.app.data.api.*
import `in`.kanoonsaathi.app.data.repository.AuthRepository
import `in`.kanoonsaathi.app.data.repository.UserRepository
import `in`.kanoonsaathi.app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val role: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
    // Student fields
    val fullName: String = "",
    val collegeName: String = "",
    val yearOfStudy: String = "1",
    val semester: String = "1",
    val examTarget: String = "",
    val selectedSubjects: Set<String> = emptySet(),
    // Faculty fields
    val institutionName: String = "",
    val designation: String = "",
    val barCouncilId: String = "",
    val selectedSubjectsTaught: Set<String> = emptySet(),
    // Curious fields
    val displayName: String = "",
    val selectedInterests: Set<String> = emptySet(),
    val disclaimerAccepted: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(role = authRepo.getRole() ?: "CURIOUS") }
    }

    fun onFullName(v: String)          = _uiState.update { it.copy(fullName = v) }
    fun onCollegeName(v: String)       = _uiState.update { it.copy(collegeName = v) }
    fun onYear(v: String)              = _uiState.update { it.copy(yearOfStudy = v) }
    fun onSemester(v: String)          = _uiState.update { it.copy(semester = v) }
    fun onExamTarget(v: String)        = _uiState.update { it.copy(examTarget = v) }
    fun onInstitutionName(v: String)   = _uiState.update { it.copy(institutionName = v) }
    fun onDesignation(v: String)       = _uiState.update { it.copy(designation = v) }
    fun onBarCouncilId(v: String)      = _uiState.update { it.copy(barCouncilId = v) }
    fun onDisplayName(v: String)       = _uiState.update { it.copy(displayName = v) }
    fun onDisclaimerAccepted(v: Boolean) = _uiState.update { it.copy(disclaimerAccepted = v) }

    fun toggleSubject(subject: String) {
        _uiState.update {
            val current = it.selectedSubjects.toMutableSet()
            if (subject in current) current.remove(subject) else current.add(subject)
            it.copy(selectedSubjects = current)
        }
    }

    fun toggleSubjectTaught(subject: String) {
        _uiState.update {
            val current = it.selectedSubjectsTaught.toMutableSet()
            if (subject in current) current.remove(subject) else current.add(subject)
            it.copy(selectedSubjectsTaught = current)
        }
    }

    fun toggleInterest(interest: String) {
        _uiState.update {
            val current = it.selectedInterests.toMutableSet()
            if (interest in current) current.remove(interest) else current.add(interest)
            it.copy(selectedInterests = current)
        }
    }

    fun submit() {
        val s = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result: Result<Unit> = when (s.role) {
                "STUDENT" -> {
                    if (s.fullName.isBlank() || s.collegeName.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, error = "Please fill all required fields") }
                        return@launch
                    }
                    userRepo.submitStudentOnboarding(
                        StudentOnboardingRequest(
                            fullName = s.fullName.trim(),
                            collegeName = s.collegeName.trim(),
                            yearOfStudy = s.yearOfStudy.toIntOrNull() ?: 1,
                            semester = s.semester.toIntOrNull() ?: 1,
                            subjectsOfInterest = s.selectedSubjects.toList(),
                            examTarget = s.examTarget.ifBlank { null },
                        )
                    )
                }
                "FACULTY" -> {
                    if (s.fullName.isBlank() || s.institutionName.isBlank() || s.designation.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, error = "Please fill all required fields") }
                        return@launch
                    }
                    userRepo.submitFacultyOnboarding(
                        FacultyOnboardingRequest(
                            fullName = s.fullName.trim(),
                            institutionName = s.institutionName.trim(),
                            designation = s.designation.trim(),
                            subjectsTaught = s.selectedSubjectsTaught.toList(),
                            barCouncilId = s.barCouncilId.ifBlank { null },
                        )
                    )
                }
                else -> { // CURIOUS
                    if (s.displayName.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, error = "Please enter your name") }
                        return@launch
                    }
                    if (!s.disclaimerAccepted) {
                        _uiState.update { it.copy(isLoading = false, error = "Please accept the disclaimer to continue") }
                        return@launch
                    }
                    userRepo.submitCuriousOnboarding(
                        CuriousOnboardingRequest(
                            displayName = s.displayName.trim(),
                            areasOfInterest = s.selectedInterests.toList(),
                            disclaimerAccepted = true,
                        )
                    )
                }
            }
            when (result) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isDone = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
