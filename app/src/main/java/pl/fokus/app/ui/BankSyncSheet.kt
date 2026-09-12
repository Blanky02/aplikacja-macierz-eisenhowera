package pl.fokus.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import pl.fokus.app.BuildConfig
import pl.fokus.app.bank.BankConnection
import pl.fokus.app.bank.BankSyncUiState
import pl.fokus.app.bank.BankSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSyncSheet(
    viewModel: BankSyncViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var email by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.browserUrl) {
        val url = state.browserUrl ?: return@LaunchedEffect
        viewModel.consumeBrowserUrl()
        openExternalUrl(context, url)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Synchronizacja bankowa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Odczyt kont i transakcji przez Salt Edge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) { Text("×", style = MaterialTheme.typography.headlineMedium) }
                }
            }

            item { BankSecurityNote() }

            if (!state.isConfigured) {
                item {
                    InfoCard(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Backend nie jest skonfigurowany",
                        body = "Ustaw fokusBackendUrl albo zmienną FOKUS_BACKEND_URL podczas budowania aplikacji.",
                        isError = true,
                    )
                }
            } else if (!state.isSignedIn) {
                item {
                    Text("Zaloguj się do Fokus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Konto jest potrzebne tylko do synchronizacji bankowej. Budżet lokalny działa bez logowania.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Adres e-mail") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        enabled = !state.isBusy,
                    )
                }
                item {
                    Button(
                        onClick = { viewModel.requestEmailLogin(email) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy && email.contains('@'),
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Wyślij link logowania")
                    }
                }
                item {
                    Text("albo", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    OutlinedButton(
                        onClick = {
                            scope.launch { signInWithGoogle(context, viewModel) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(),
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) "Google — brak konfiguracji" else "Zaloguj przez Google")
                    }
                }
                if (state.emailSent) {
                    item {
                        InfoCard(
                            icon = Icons.Outlined.CheckCircle,
                            title = "Sprawdź swoją skrzynkę",
                            body = "Link jest jednorazowy i ważny 15 minut. Po kliknięciu aplikacja dokończy logowanie.",
                        )
                    }
                }
            } else {
                item {
                    SignedInCard(
                        email = state.userEmail.orEmpty(),
                        busy = state.isBusy,
                        onLogout = viewModel::logout,
                    )
                }
                item {
                    Button(
                        onClick = viewModel::connectBank,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy,
                    ) {
                        Icon(Icons.Outlined.AccountBalance, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Połącz bank")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::sync,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy && state.connections.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Synchronizuj teraz")
                    }
                }
                if (state.connections.isEmpty()) {
                    item {
                        InfoCard(
                            icon = Icons.Outlined.AccountBalance,
                            title = "Nie masz jeszcze połączonego banku",
                            body = "Po kliknięciu otworzy się bezpieczne okno Salt Edge. Dane logowania wpisujesz wyłącznie na stronie banku.",
                        )
                    }
                } else {
                    item {
                        Text("Połączone instytucje", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.connections, key = { it.id }) { connection ->
                        ConnectionRow(connection)
                    }
                }
                state.lastSync?.let { result ->
                    item {
                        Text(
                            "Ostatnia synchronizacja: ${result.transactions} transakcji, ${result.accounts} rachunków.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.message?.let { message ->
                item { StatusText(message, isError = false) }
            }
            state.error?.let { error ->
                item { StatusText(error, isError = true) }
            }
            if (state.isBusy) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun BankSecurityNote() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(9.dp))
            Column {
                Text("Odczyt bez wykonywania płatności", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Fokus pobiera tylko dane udostępnione przez bank. Nie przechowujemy haseł bankowych i nie możemy zlecać przelewów.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SignedInCard(email: String, busy: Boolean, onLogout: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Zalogowano", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(email, fontWeight = FontWeight.Medium)
            }
            TextButton(onClick = onLogout, enabled = !busy) { Text("Wyloguj") }
        }
    }
}

@Composable
private fun ConnectionRow(connection: BankConnection) {
    Card {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(9.dp))
            Column(Modifier.weight(1f)) {
                Text(connection.providerName, fontWeight = FontWeight.Medium)
                Text(connection.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.CloudSync, contentDescription = "Synchronizacja", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, isError: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(9.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(11.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private suspend fun signInWithGoogle(context: Context, viewModel: BankSyncViewModel) {
    try {
        val credentialManager = CredentialManager.create(context)
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.signInWithGoogle(googleCredential.idToken)
        } else {
            viewModel.showError("Nie udało się odczytać poświadczenia Google.")
        }
    } catch (error: GetCredentialException) {
        viewModel.showError("Logowanie Google zostało anulowane lub jest niedostępne.")
    } catch (error: Exception) {
        viewModel.showError(error.message ?: "Logowanie Google nie powiodło się.")
    }
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        // The sheet will remain visible and the next state update can show a useful error.
    }
}
