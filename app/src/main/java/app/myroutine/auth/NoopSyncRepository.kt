package app.myroutine.auth

import app.myroutine.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoopSyncRepository @Inject constructor() : SyncRepository {
    override val isAvailable: Boolean = false
    override suspend fun push(): Result<Unit> = Result.success(Unit)
    override suspend fun pull(): Result<Unit> = Result.success(Unit)
}
