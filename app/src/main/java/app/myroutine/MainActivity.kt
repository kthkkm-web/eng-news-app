package app.myroutine

import android.content.Intent
import android.os.Bundle
import app.myroutine.core.DeepLinks
import app.myroutine.widget.DeepLink
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.ui.MainViewModel
import app.myroutine.ui.navigation.AppNavHost
import app.myroutine.ui.theme.MyRoutineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DeepLinks.push(intent?.getStringExtra(DeepLink.EXTRA_ROUTE))
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            MyRoutineTheme(themeMode = settings.themeMode) {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLinks.push(intent.getStringExtra(DeepLink.EXTRA_ROUTE))
    }
}
