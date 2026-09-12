package pl.fokus.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ExchangeRateClient {
    suspend fun fetchRate(from: String, to: String): Double? = withContext(Dispatchers.IO) {
        if (from == to) return@withContext 1.0
        runCatching {
            val connection = URL("https://api.frankfurter.app/latest?from=$from&to=$to")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 4_000
            connection.readTimeout = 4_000
            connection.requestMethod = "GET"
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            Regex("\\\"$to\\\"\\s*:\\s*([0-9.]+)").find(body)?.groupValues?.get(1)?.toDouble()
        }.getOrNull()
    }
}
