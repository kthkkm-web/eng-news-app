package app.myroutine.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeAll(userId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder, createdAt")
    suspend fun getAll(userId: String): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id AND deletedAt IS NULL")
    suspend fun get(id: String): RoutineEntity?

    @Query("SELECT MAX(sortOrder) FROM routines WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun maxSortOrder(userId: String): Int?

    @Upsert suspend fun upsert(entity: RoutineEntity)

    @Query("UPDATE routines SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE routines SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE routines SET sortOrder = :order, updatedAt = :now WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int, now: Long)

    // 백업용(삭제 포함 전체)
    @Query("SELECT * FROM routines") suspend fun dumpAll(): List<RoutineEntity>
    @Upsert suspend fun upsertAll(list: List<RoutineEntity>)
}

@Dao
interface RepeatRuleDao {
    @Query("SELECT * FROM repeat_rules WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder")
    fun observeAll(userId: String): Flow<List<RepeatRuleEntity>>

    @Query("SELECT * FROM repeat_rules WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder")
    suspend fun getAll(userId: String): List<RepeatRuleEntity>

    @Query("SELECT * FROM repeat_rules WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY sortOrder")
    suspend fun getFor(routineId: String): List<RepeatRuleEntity>

    @Upsert suspend fun upsert(entity: RepeatRuleEntity)

    @Query("UPDATE repeat_rules SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE repeat_rules SET deletedAt = :now, updatedAt = :now WHERE routineId = :routineId AND deletedAt IS NULL")
    suspend fun softDeleteFor(routineId: String, now: Long)

    @Query("SELECT * FROM repeat_rules") suspend fun dumpAll(): List<RepeatRuleEntity>
    @Upsert suspend fun upsertAll(list: List<RepeatRuleEntity>)
}

@Dao
interface RoutineLogDao {
    @Query("SELECT * FROM routine_logs WHERE userId = :userId AND deletedAt IS NULL AND date BETWEEN :from AND :to")
    fun observeRange(userId: String, from: String, to: String): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE userId = :userId AND deletedAt IS NULL AND date BETWEEN :from AND :to")
    suspend fun getRange(userId: String, from: String, to: String): List<RoutineLogEntity>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY date")
    fun observeFor(routineId: String): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY date")
    suspend fun getFor(routineId: String): List<RoutineLogEntity>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND date = :date")
    suspend fun find(routineId: String, date: String): RoutineLogEntity?

    @Upsert suspend fun upsert(entity: RoutineLogEntity)

    @Query("SELECT * FROM routine_logs") suspend fun dumpAll(): List<RoutineLogEntity>
    @Upsert suspend fun upsertAll(list: List<RoutineLogEntity>)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeAll(userId: String): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM tags WHERE userId = :userId")
    suspend fun countAll(userId: String): Int

    @Query("SELECT MAX(sortOrder) FROM tags WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun maxSortOrder(userId: String): Int?

    @Query("SELECT * FROM tags WHERE id = :id") suspend fun get(id: String): TagEntity?

    @Upsert suspend fun upsert(entity: TagEntity)

    @Query("UPDATE tags SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE tags SET sortOrder = :order, updatedAt = :now WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int, now: Long)

    @Query("SELECT * FROM tags") suspend fun dumpAll(): List<TagEntity>
    @Upsert suspend fun upsertAll(list: List<TagEntity>)
}

@Dao
interface RoutineTagDao {
    @Query("SELECT * FROM routine_tags WHERE userId = :userId AND deletedAt IS NULL")
    fun observeAll(userId: String): Flow<List<RoutineTagEntity>>

    @Query("SELECT * FROM routine_tags WHERE routineId = :routineId")
    suspend fun getAllFor(routineId: String): List<RoutineTagEntity>

    @Upsert suspend fun upsert(entity: RoutineTagEntity)

    @Query("UPDATE routine_tags SET deletedAt = :now, updatedAt = :now WHERE tagId = :tagId AND deletedAt IS NULL")
    suspend fun softDeleteByTag(tagId: String, now: Long)

    @Query("SELECT * FROM routine_tags") suspend fun dumpAll(): List<RoutineTagEntity>
    @Upsert suspend fun upsertAll(list: List<RoutineTagEntity>)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE userId = :userId AND date = :date AND deletedAt IS NULL ORDER BY done, time IS NULL, time, sortOrder, createdAt")
    fun observeFor(userId: String, date: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE userId = :userId AND date = :date AND deletedAt IS NULL ORDER BY done, time IS NULL, time, sortOrder, createdAt")
    suspend fun getFor(userId: String, date: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id") suspend fun get(id: String): TodoEntity?

    @Upsert suspend fun upsert(entity: TodoEntity)

    @Query("UPDATE todos SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM todos") suspend fun dumpAll(): List<TodoEntity>
    @Upsert suspend fun upsertAll(list: List<TodoEntity>)
}
