package app.myroutine.data.repository

import androidx.room.withTransaction
import app.myroutine.core.DataChanges
import app.myroutine.core.TimeProvider
import app.myroutine.core.newId
import app.myroutine.data.db.AppDatabase
import app.myroutine.data.db.TagEntity
import app.myroutine.data.db.toDomain
import app.myroutine.domain.model.Tag
import app.myroutine.domain.repository.AuthRepository
import app.myroutine.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val auth: AuthRepository,
    private val time: TimeProvider,
    private val changes: DataChanges,
) : TagRepository {
    private val dao = db.tagDao()
    private val uid get() = auth.currentUserId()

    override fun observeTags(): Flow<List<Tag>> = dao.observeAll(uid).map { l -> l.map { it.toDomain() } }

    override suspend fun saveTag(tag: Tag): String {
        val now = time.nowMillis()
        val existing = dao.get(tag.id)
        val order = existing?.sortOrder ?: ((dao.maxSortOrder(uid) ?: -1) + 1)
        dao.upsert(TagEntity(tag.id, uid, tag.name.trim(), tag.emoji, order, existing?.createdAt ?: now, now))
        changes.notifyChanged()
        return tag.id
    }

    override suspend fun deleteTag(id: String) {
        val now = time.nowMillis()
        db.withTransaction {
            dao.softDelete(id, now)
            db.routineTagDao().softDeleteByTag(id, now)
        }
        changes.notifyChanged()
    }

    override suspend fun reorder(idsInOrder: List<String>) {
        val now = time.nowMillis()
        db.withTransaction { idsInOrder.forEachIndexed { i, id -> dao.setSortOrder(id, i, now) } }
        changes.notifyChanged()
    }

    /** 최초 실행 시 기본 태그 7개. 사용자가 지운 태그는 되살리지 않는다(삭제 포함 개수로 판단). */
    override suspend fun seedDefaultsIfEmpty() {
        if (dao.countAll(uid) > 0) return
        val now = time.nowMillis()
        DEFAULT_TAGS.forEachIndexed { i, (emoji, name) ->
            dao.upsert(TagEntity(newId(), uid, name, emoji, i, now, now))
        }
    }

    companion object {
        val DEFAULT_TAGS = listOf(
            "☀️" to "미라클 모닝",
            "🌱" to "건강",
            "💪" to "운동",
            "✨" to "청소",
            "🍽️" to "음식",
            "🛌" to "취침",
            "🚫" to "금지",
        )
    }
}
