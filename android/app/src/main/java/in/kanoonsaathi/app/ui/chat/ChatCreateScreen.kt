package `in`.kanoonsaathi.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.kanoonsaathi.app.ui.theme.NavyDeep

/**
 * Intermediate screen that creates a session then immediately navigates to it.
 * Shows a loading spinner while the API call completes.
 * User never sees this for more than ~500ms.
 */
@Composable
fun ChatCreateScreen(
    modeId: String,
    modeName: String,
    onSessionCreated: (sessionId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChatCreateViewModel = hiltViewModel(),
) {
    LaunchedEffect(modeId) {
        viewModel.createSession(modeId) { sessionId ->
            onSessionCreated(sessionId)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NavyDeep)
            Spacer(Modifier.height(12.dp))
            Text("Starting chat...", color = NavyDeep)
        }
    }
}
