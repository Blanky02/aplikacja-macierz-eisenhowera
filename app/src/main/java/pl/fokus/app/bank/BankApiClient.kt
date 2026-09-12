package pl.fokus.app.bank

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pl.fokus.app.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class BankApiClient(
    private val authStore: BankAuthStore,
) {
    private val baseUrl: String
        get() = BuildConfig.FOKUS_BACKEND_URL.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    suspend fun loginWithGoogle(idToken: String): BankUser = withContext(Dispatchers.IO) {
        val json = request(
            path = "/api/auth/google",
            method = "POST",
            body = JSONObject().put("idToken", idToken),
            authenticated = false,
        )
        saveSession(json)
    }

    suspend fun requestEmailLogin(email: String): EmailLoginResult = withContext(Dispatchers.IO) {
        val json = request(
            path = "/api/auth/email/request",
            method = "POST",
            body = JSONObject().put("email", email),
            authenticated = false,
        )
        EmailLoginResult(json.optStringOrNull("devVerificationUrl"))
    }

    suspend fun exchangeEmailCode(code: String): BankUser = withContext(Dispatchers.IO) {
        val json = request(
            path = "/api/auth/email/exchange",
            method = "POST",
            body = JSONObject().put("code", code),
            authenticated = false,
        )
        saveSession(json)
    }

    suspend fun listConnections(): List<BankConnection> = withContext(Dispatchers.IO) {
        val data = request("/api/bank/connections", "GET").optJSONArray("data") ?: JSONArray()
        buildList(data.length()) {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                add(
                    BankConnection(
                        id = item.optString("id"),
                        providerName = item.optString("provider_name", "Bank"),
                        status = item.optString("status", "connected"),
                        lastSyncAt = item.optStringOrNull("last_sync_at"),
                    ),
                )
            }
        }
    }

    suspend fun startConnection(historyDays: Int = 3650): BankConnectSession = withContext(Dispatchers.IO) {
        val json = request(
            path = "/api/bank/connect",
            method = "POST",
            body = JSONObject().put("historyDays", historyDays.coerceIn(1, 3650)),
        )
        BankConnectSession(
            authorizationUrl = json.optString("authorizationUrl"),
            expiresAt = json.optStringOrNull("expiresAt"),
            historyDays = json.optInt("historyDays", historyDays),
        )
    }

    suspend fun sync(): BankSyncResult = withContext(Dispatchers.IO) {
        val json = request("/api/bank/sync", "POST", JSONObject())
        BankSyncResult(
            accounts = json.optInt("accounts"),
            transactions = json.optInt("transactions"),
        )
    }

    suspend fun confirmQuadrant(transactionId: String, quadrantIndex: Int) = withContext(Dispatchers.IO) {
        request(
            path = "/api/bank/transactions/${transactionId.encodePathSegment()}/quadrant",
            method = "PATCH",
            body = JSONObject().put("quadrantIndex", quadrantIndex.coerceIn(0, 3)),
        )
    }

    suspend fun deleteConnection(connectionId: String) = withContext(Dispatchers.IO) {
        request(
            path = "/api/bank/connections/${connectionId.encodePathSegment()}",
            method = "DELETE",
        )
    }

    private fun saveSession(json: JSONObject): BankUser {
        val userJson = json.optJSONObject("user") ?: throw BankApiException(500, "Brak danych użytkownika")
        val token = json.optString("accessToken")
        if (token.isBlank()) throw BankApiException(500, "Brak tokenu sesji")
        val user = BankUser(userJson.optString("id"), userJson.optString("email"))
        authStore.saveSession(user, token)
        return user
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
    ): JSONObject {
        if (!isConfigured) throw BankApiException(0, "Skonfiguruj adres backendu Fokus.")
        val url = URL("$baseUrl$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("X-Fokus-Client", "android/${BuildConfig.VERSION_NAME}")
            if (authenticated) {
                val token = authStore.accessToken ?: throw BankApiException(401, "Zaloguj się ponownie.")
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) doOutput = true
        }

        try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299) {
                throw BankApiException(status, parseError(response, status))
            }
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } catch (error: BankApiException) {
            throw error
        } catch (error: Exception) {
            throw BankApiException(0, error.message ?: "Nie udało się połączyć z backendem.")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseError(response: String, status: Int): String {
        return runCatching {
            val json = JSONObject(response)
            json.optString("message").ifBlank { json.optString("error") }
        }.getOrNull().orEmpty().ifBlank {
            "Backend zwrócił błąd HTTP $status."
        }
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    optString(key).takeUnless { it.isBlank() || it == JSONObject.NULL.toString() }

private fun String.encodePathSegment(): String =
    java.net.URLEncoder.encode(this, StandardCharsets.UTF_8.toString()).replace("+", "%20")
