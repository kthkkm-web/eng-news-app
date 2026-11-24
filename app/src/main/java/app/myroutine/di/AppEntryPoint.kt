package app.myroutine.di

import android.content.Context
import app.myroutine.core.DataChanges
import app.myroutine.core.TimeProvider
import app.myroutine.data.settings.SettingsRepository
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.TagRepository
import app.myroutine.domain.repository.TodoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Hilt 주입이 안 되는 곳(Glance 위젯, BroadcastReceiver 내부 헬퍼, 테스트)에서 의존성 접근. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun routines(): RoutineRepository
    fun tags(): TagRepository
    fun todos(): TodoRepository
    fun settings(): SettingsRepository
    fun time(): TimeProvider
    fun dataChanges(): DataChanges
    fun templates(): app.myroutine.ui.templates.TemplateRepository
    fun backup(): app.myroutine.data.backup.BackupManager

    companion object {
        fun get(context: Context): AppEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, AppEntryPoint::class.java)
    }
}
