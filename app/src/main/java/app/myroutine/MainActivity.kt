package app.myroutine

import android.os.Bundle
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
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            MyRoutineTheme(themeMode = settings.themeMode) {
                AppNavHost()
            }
        }
    }
}
