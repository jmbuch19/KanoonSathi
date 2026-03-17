package `in`.kanoonsaathi.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.kanoonsaathi.app.ui.theme.*

private val LAW_SUBJECTS = listOf(
    "Constitutional Law", "Contract Law", "Criminal Law", "Tort Law",
    "Family Law", "Property Law", "Administrative Law", "IPR",
    "Corporate Law", "Labour Law", "Environmental Law", "Evidence Act",
    "CPC", "CrPC", "IPC",
)

private val EXAM_TARGETS = listOf("CLAT PG", "AIBE", "Judicial Services", "UPSC (Law)", "None")

private val DESIGNATIONS = listOf(
    "Assistant Professor", "Associate Professor", "Professor", "HOD",
    "Dean", "Visiting Faculty", "Advocate", "Other",
)

private val CURIOUS_INTERESTS = listOf(
    "Consumer Rights", "Property Disputes", "Criminal Law",
    "Family & Marriage", "Labour Rights", "Business Law",
    "RTI & PILs", "Traffic & Motor", "Cyber Law", "General Awareness",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onComplete()
    }

    LaunchedEffect(uiState.error) {
        // Error handled inline
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Set Up Your Profile",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Role badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (uiState.role) {
                    "STUDENT" -> StudentBlue.copy(alpha = 0.12f)
                    "FACULTY" -> FacultyGreen.copy(alpha = 0.12f)
                    else      -> CuriousOrange.copy(alpha = 0.12f)
                },
            ) {
                Text(
                    text = when (uiState.role) {
                        "STUDENT" -> "🎓 Law Student"
                        "FACULTY" -> "👨‍🏫 Law Faculty"
                        else      -> "🔍 Curious Learner"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (uiState.role) {
                        "STUDENT" -> StudentBlue
                        "FACULTY" -> FacultyGreen
                        else      -> CuriousOrange
                    },
                )
            }

            Text(
                "Tell us about yourself so we can personalise your experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            when (uiState.role) {
                "STUDENT" -> StudentFields(uiState, viewModel)
                "FACULTY" -> FacultyFields(uiState, viewModel)
                else      -> CuriousFields(uiState, viewModel)
            }

            // Error
            uiState.error?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ErrorRed.copy(alpha = 0.1f),
                ) {
                    Text(
                        err,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Continue →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Skip for curious users
            if (uiState.role == "CURIOUS") {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Skip for now", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun StudentFields(s: OnboardingUiState, vm: OnboardingViewModel) {
    OnboardingTextField("Full Name *", s.fullName, vm::onFullName)
    OnboardingTextField("College / Law School *", s.collegeName, vm::onCollegeName)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownField(
            label = "Year *",
            options = (1..5).map { it.toString() },
            selected = s.yearOfStudy,
            onSelect = vm::onYear,
            modifier = Modifier.weight(1f),
        )
        DropdownField(
            label = "Semester *",
            options = (1..10).map { it.toString() },
            selected = s.semester,
            onSelect = vm::onSemester,
            modifier = Modifier.weight(1f),
        )
    }

    DropdownField(
        label = "Exam Target (optional)",
        options = EXAM_TARGETS,
        selected = s.examTarget.ifBlank { EXAM_TARGETS.last() },
        onSelect = vm::onExamTarget,
    )

    SubjectChips(
        label = "Subjects of Interest",
        options = LAW_SUBJECTS,
        selected = s.selectedSubjects,
        onToggle = vm::toggleSubject,
    )
}

@Composable
private fun FacultyFields(s: OnboardingUiState, vm: OnboardingViewModel) {
    OnboardingTextField("Full Name *", s.fullName, vm::onFullName)
    OnboardingTextField("Institution / University *", s.institutionName, vm::onInstitutionName)

    DropdownField(
        label = "Designation *",
        options = DESIGNATIONS,
        selected = s.designation.ifBlank { DESIGNATIONS.first() },
        onSelect = vm::onDesignation,
    )

    OnboardingTextField("Bar Council ID (optional)", s.barCouncilId, vm::onBarCouncilId)

    SubjectChips(
        label = "Subjects You Teach",
        options = LAW_SUBJECTS,
        selected = s.selectedSubjectsTaught,
        onToggle = vm::toggleSubjectTaught,
    )
}

@Composable
private fun CuriousFields(s: OnboardingUiState, vm: OnboardingViewModel) {
    OnboardingTextField("Your Name *", s.displayName, vm::onDisplayName)

    SubjectChips(
        label = "Areas of Interest (optional)",
        options = CURIOUS_INTERESTS,
        selected = s.selectedInterests,
        onToggle = vm::toggleInterest,
    )

    // Disclaimer card
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "⚠️ Educational Disclaimer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "KanoonSaathi provides legal information for educational purposes only. " +
                "It does NOT provide legal advice and should NOT be treated as a substitute " +
                "for a qualified lawyer. Always consult a licensed advocate for your specific situation.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF78350F),
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { vm.onDisclaimerAccepted(!s.disclaimerAccepted) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = s.disclaimerAccepted,
                    onCheckedChange = vm::onDisclaimerAccepted,
                    colors = CheckboxDefaults.colors(checkedColor = NavyDeep),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "I understand this is for educational use only *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun OnboardingTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NavyDeep,
            focusedLabelColor = NavyDeep,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NavyDeep,
                focusedLabelColor = NavyDeep,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SubjectChips(
    label: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        // Wrap chips in a flow-like grid (2 per row)
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { subject ->
                    val isSelected = subject in selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NavyDeep else SurfaceCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NavyDeep else Color(0xFFD1D5DB),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { onToggle(subject) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
