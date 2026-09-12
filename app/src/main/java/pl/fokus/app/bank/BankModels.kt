package pl.fokus.app.bank

/** Data returned by the Fokus Worker. Amounts are in provider currency minor units. */
data class BankUser(
    val id: String,
    val email: String,
)

data class BankConnection(
    val id: String,
    val providerName: String,
    val status: String,
    val lastSyncAt: String?,
)

data class BankTransaction(
    val id: String,
    val accountId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val description: String,
    val merchantName: String?,
    val madeOn: String,
    val status: String,
    val quadrantIndex: Int?,
    val quadrantConfirmed: Boolean,
)

data class BankSyncResult(
    val accounts: Int,
    val transactions: Int,
)

data class BankConnectSession(
    val authorizationUrl: String,
    val expiresAt: String?,
    val historyDays: Int,
)

data class EmailLoginResult(
    val devVerificationUrl: String?,
)

class BankApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)
