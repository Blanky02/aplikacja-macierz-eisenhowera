package pl.fokus.app.bank

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fokus.app.BuildConfig


data class BankSyncUiState(
    val isConfigured: Boolean = BuildConfig.FOKUS_BACKEND_URL.isNotBlank(),
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val isBusy: Boolean = false,
    val emailSent: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val browserUrl: String? = null,
    val connections: List<BankConnection> = emptyList(),
    val lastSync: BankSyncResult? = null,
)

class BankSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val authStore = BankAuthStore(application)
    private val api = BankApiClient(authStore)
    private val _state = MutableStateFlow(
        BankSyncUiState(
            isConfigured = api.isConfigured,
            isSignedIn = authStore.accessToken != null,
            userEmail = authStore.userEmail,
        ),
    )
    val state: StateFlow<BankSyncUiState> = _state.asStateFlow()

    init {
        if (authStore.accessToken != null) refreshConnections()
    }

    fun signInWithGoogle(idToken: String) = launchRequest {
        val user = api.loginWithGoogle(idToken)
        signedInMessage(user.email)
        refreshConnectionsInternal()
    }

    fun requestEmailLogin(email: String) = launchRequest {
        val result = api.requestEmailLogin(email.trim())
        _state.update {
            it.copy(
                emailSent = true,
                message = "Jeśli adres jest poprawny, wysłaliśmy link logowania.",
                browserUrl = result.devVerificationUrl,
            )
        }
    }

    fun exchangeEmailCode(code: String) = launchRequest {
        val user = api.exchangeEmailCode(code)
        signedInMessage(user.email)
        refreshConnectionsInternal()
    }

    fun connectBank() = launchRequest {
        val session = api.startConnection()
        _state.update {
            it.copy(
                message = "Otwieramy bezpieczne okno banku. Nie wpisuj danych bankowych w Fokus.",
                browserUrl = session.authorizationUrl,
            )
        }
    }

    fun sync() = launchRequest {
        val result = api.sync()
        refreshConnectionsInternal()
        _state.update {
            it.copy(
                lastSync = result,
                message = "Synchronizacja zakończona: ${result.transactions} transakcji.",
            )
        }
    }

    fun refreshConnections() = launchRequest {
        refreshConnectionsInternal()
    }

    fun logout() {
        authStore.clear()
        _state.value = BankSyncUiState(isConfigured = api.isConfigured)
    }

    fun consumeBrowserUrl(): String? {
        val url = _state.value.browserUrl
        if (url != null) _state.update { it.copy(browserUrl = null) }
        return url
    }

    fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    private suspend fun refreshConnectionsInternal() {
        if (authStore.accessToken == null) return
        val connections = api.listConnections()
        _state.update {
            it.copy(
                isSignedIn = true,
                userEmail = authStore.userEmail,
                connections = connections,
            )
        }
    }

    private fun signedInMessage(email: String) {
        _state.update {
            it.copy(
                isSignedIn = true,
                userEmail = email,
                emailSent = false,
                message = "Zalogowano jako $email.",
            )
        }
    }

    private fun launchRequest(block: suspend () -> Unit) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            try {
                block()
            } catch (error: Exception) {
                if (error is BankApiException && error.statusCode == 401) {
                    authStore.clear()
                    _state.update { it.copy(isSignedIn = false, userEmail = null, connections = emptyList()) }
                }
                _state.update { it.copy(error = error.message ?: "Wystąpił nieoczekiwany błąd.") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BankSyncViewModel::class.java)) {
                return BankSyncViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
