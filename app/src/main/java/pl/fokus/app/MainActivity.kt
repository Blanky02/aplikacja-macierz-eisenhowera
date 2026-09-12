package pl.fokus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import pl.fokus.app.bank.BankSyncViewModel
import pl.fokus.app.ui.FokusApp
import pl.fokus.app.ui.FokusTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as FokusApplication).repository)
    }
    private val bankViewModel: BankSyncViewModel by viewModels {
        BankSyncViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleEmailAuthIntent(intent)
        setContent {
            FokusTheme {
                FokusApp(viewModel, bankViewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmailAuthIntent(intent)
    }

    private fun handleEmailAuthIntent(intent: android.content.Intent?) {
        val code = intent?.data
            ?.takeIf { it.scheme == "fokus" && it.host == "auth" && it.path == "/email" }
            ?.getQueryParameter("code")
        if (!code.isNullOrBlank()) bankViewModel.exchangeEmailCode(code)
    }
}
