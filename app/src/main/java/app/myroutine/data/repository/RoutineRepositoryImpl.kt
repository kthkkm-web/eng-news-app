package app.myroutine.data.repository

import androidx.room.withTransaction
import app.myroutine.core.DataChanges
import app.myroutine.core.TimeProvider
import app.myroutine.core.newId
import app.myroutine.data.db.AppDatabase
import app.myroutine.data.db.RoutineLogEntity
import app.myroutine.data.db.RoutineTagEntity
import app.myroutine.data.db.key
import app.myroutine.data.db.toDomain
import app.myroutine.data.db.toEntity
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.repository.AuthRepository
import app.myroutine.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val auth: AuthRepository,
    private val time: TimeProvider,
    private val changes: DataChanges,
) : RoutineRepository {

    private val routineDao = db.routineDao()
    private val ruleDao = db.repeatRuleDao()
    private val logDao = db.routineLogDao()
    private val linkDao = db.routineTagDao()
    private val uid get() = auth.currentUserId()

    override fun observeRoutines(): Flow<List<Routine>> = combine(
        routineDao.observeAll(uid),
        ruleDao.observeAll(uid),
        linkDao.observeAll(uid),
    ) { routines, rules, links ->
        val rulesBy = rules.groupBy { it.routineId }
        val tagsBy = links.groupBy({ it.routineId }, { it.tagId })
        routines.map { it.toDomain(rulesBy[it.id].orEmpty(), tagsBy[it.id].orEmpty()) }
    }

    override suspend fun getRoutine(id: String): Routine? {
        val e = routineDao.get(id) ?: return null
        val tags = linkDao.getAllFor(id).filter { it.deletedAt == null }.map { it.tagId }
        return e.toDomain(ruleDao.getFor(id), tags)
    }

    override suspend fun saveRoutine(routine: Routine): String {
        val now = time.nowMillis()
        db.withTransaction {
            val existing = routineDao.get(routine.id)
            val order = existing?.sortOrder ?: ((routineDao.maxSortOrder(uid) ?: -1) + 1)
            routineDao.upsert(
                routine.copy(sortOrder = order).toEntity(uid, now, existing?.createdAt ?: now),
            )

            // 규칙: 전달된 목록이 정답. 빠진 규칙은 soft delete.
            val oldRules = ruleDao.getFor(routine.id).associateBy { it.id }
            routine.rules.forEachIndexed { i, rule ->
                ruleDao.upsert(rule.toEntity(uid, routine.id, i, now, oldRules[rule.id]?.createdAt ?: now))
            }
            val keep = routine.rules.map { it.id }.toSet()
            oldRules.keys.filter { it !in keep }.forEach { ruleDao.softDelete(it, now) }

            // 태그 연결
            val links = linkDao.getAllFor(routine.id).associateBy { it.tagId }
            val wanted = routine.tagIds.toSet()
            links.values.forEach { link ->
                val shouldLive = link.tagId in wanted
                val alive = link.deletedAt == null
                if (shouldLive != alive) {
                    linkDao.upsert(link.copy(deletedAt = if (shouldLive) null else now, updatedAt = now))
                }
            }
            (wanted - links.keys).forEach { tagId ->
                linkDao.upsert(RoutineTagEntity(newId(), uid, routine.id, tagId, now, now))
            }
        }
        changes.notifyChanged()
        return routine.id
    }

    override suspend fun deleteRoutine(id: String) {
        val now = time.nowMillis()
        db.withTransaction {
            routineDao.softDelete(id, now)
            ruleDao.softDeleteFor(id, now)
        }
        changes.notifyChanged()
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        routineDao.setArchived(id, archived, time.nowMillis())
        changes.notifyChanged()
    }

    override suspend fun reorder(idsInOrder: List<String>) {
        val now = time.nowMillis()
        db.withTransaction { idsInOrder.forEachIndexed { i, id -> routineDao.setSortOrder(id, i, now) } }
        changes.notifyChanged()
    }

    override fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<RoutineLog>> =
        logDao.observeRange(uid, from.key(), to.key()).map { list -> list.map { it.toDomain() } }

    override suspend fun getLogs(from: LocalDate, to: LocalDate): List<RoutineLog> =
        logDao.getRange(uid, from.key(), to.key()).map { it.toDomain() }

    override fun observeLogsOf(routineId: String): Flow<List<RoutineLog>> =
        logDao.observeFor(routineId).map { list -> list.map { it.toDomain() } }

    override suspend fun addCount(routineId: String, date: LocalDate, delta: Int) =
        mutateLog(routineId, date) { log, now ->
            val newCount = (log.count + delta).coerceAtLeast(0)
            val stamps = when {
                delta > 0 -> log.timestamps + List(delta) { now }
                delta < 0 -> log.timestamps.dropLast(-delta)
                else -> log.timestamps
            }
            log.copy(count = newCount, timestamps = stamps)
        }

    override suspend fun setCount(routineId: String, date: LocalDate, count: Int) =
        mutateLog(routineId, date) { log, now ->
            val c = count.coerceAtLeast(0)
            val stamps = if (c >= log.timestamps.size) log.timestamps + List(c - log.timestamps.size) { now } else log.timestamps.take(c)
            log.copy(count = c, timestamps = stamps)
        }

    override suspend fun setValue(routineId: String, date: LocalDate, value: Double?) =
        mutateLog(routineId, date) { log, now ->
            log.copy(value = value, count = if (value != null) 1 else 0, timestamps = if (value != null) listOf(now) else emptyList())
        }

    override suspend fun resetLog(routineId: String, date: LocalDate) =
        mutateLog(routineId, date) { log, _ -> log.copy(count = 0, value = null, timestamps = emptyList()) }

    override suspend fun setLog(routineId: String, date: LocalDate, count: Int, value: Double?, timestamps: List<Long>) =
        mutateLog(routineId, date) { log, _ -> log.copy(count = count.coerceAtLeast(0), value = value, timestamps = timestamps) }

    private suspend fun mutateLog(
        routineId: String,
        date: LocalDate,
        block: (RoutineLogEntity, Long) -> RoutineLogEntity,
    ) {
        val now = time.nowMillis()
        db.withTransaction {
            val base = logDao.find(routineId, date.key())
                ?: RoutineLogEntity(newId(), uid, routineId, date.key(), 0, null, emptyList(), now, now)
            logDao.upsert(block(base, now).copy(updatedAt = now, deletedAt = null))
        }
        changes.notifyChanged()
    }
}
