package app.myroutine.di

import android.content.Context
import androidx.room.Room
import app.myroutine.auth.LocalAuthRepository
import app.myroutine.auth.NoopSyncRepository
import app.myroutine.core.SystemTimeProvider
import app.myroutine.core.TimeProvider
import app.myroutine.data.db.AppDatabase
import app.myroutine.data.repository.RoutineRepositoryImpl
import app.myroutine.data.repository.TagRepositoryImpl
import app.myroutine.data.repository.TodoRepositoryImpl
import app.myroutine.domain.repository.AuthRepository
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.SyncRepository
import app.myroutine.domain.repository.TagRepository
import app.myroutine.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // .addMigrations(...)  스키마 변경 시 여기에 추가
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {
    @Binds abstract fun auth(impl: LocalAuthRepository): AuthRepository
    @Binds abstract fun sync(impl: NoopSyncRepository): SyncRepository
    @Binds abstract fun time(impl: SystemTimeProvider): TimeProvider
    @Binds abstract fun routines(impl: RoutineRepositoryImpl): RoutineRepository
    @Binds abstract fun tags(impl: TagRepositoryImpl): TagRepository
    @Binds abstract fun todos(impl: TodoRepositoryImpl): TodoRepository
}
