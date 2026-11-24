package app.myroutine

import android.app.Application
import app.myroutine.domain.repository.TagRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyRoutineApp : Application() {
    @Inject lateinit var tagRepository: TagRepository

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { tagRepository.seedDefaultsIfEmpty() }
    }
}
