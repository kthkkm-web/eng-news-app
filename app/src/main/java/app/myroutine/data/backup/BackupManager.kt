package app.myroutine.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import app.myroutine.core.DataChanges
import app.myroutine.data.db.AppDatabase
import app.myroutine.data.db.RepeatRuleEntity
import app.myroutine.data.db.RoutineEntity
import app.myroutine.data.db.RoutineLogEntity
import app.myroutine.data.db.RoutineTagEntity
import app.myroutine.data.db.TagEntity
import app.myroutine.data.db.TodoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val schemaVersion: Int,
    val exportedAt: Long,
    val routines: List<RoutineEntity>,
    val rules: List<RepeatRuleEntity>,
    val logs: List<RoutineLogEntity>,
    val tags: List<TagEntity>,
    val routineTags: List<RoutineTagEntity>,
    val todos: List<TodoEntity>,
) {
    companion object { const val FORMAT = "myroutine-backup" }
}

data class BackupSummary(val routines: Int, val logs: Int, val tags: Int, val todos: Int)

/** 전체 DB(삭제 표시 행 포함) ↔ JSON. 복원은 기존 데이터를 모두 교체한다. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val changes: DataChanges,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    suspend fun exportTo(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val file = BackupFile(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            routines = db.routineDao().dumpAll(),
            rules = db.repeatRuleDao().dumpAll(),
            logs = db.routineLogDao().dumpAll(),
            tags = db.tagDao().dumpAll(),
            routineTags = db.routineTagDao().dumpAll(),
            todos = db.todoDao().dumpAll(),
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.encodeToString(BackupFile.serializer(), file).toByteArray()) }
            ?: error("파일을 열 수 없어요")
        file.summary()
    }

    /** 파일을 읽어 검증만 한다(복원 전 확인 화면용). */
    suspend fun read(uri: Uri): BackupFile = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("파일을 열 수 없어요")
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }.getOrElse { error("마이루틴 백업 파일이 아니에요") }
        require(file.format == BackupFile.FORMAT) { "마이루틴 백업 파일이 아니에요" }
        require(file.schemaVersion <= SCHEMA_VERSION) { "더 새로운 버전의 앱에서 만든 백업이에요. 앱을 업데이트해 주세요." }
        file
    }

    suspend fun restore(file: BackupFile): BackupSummary = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.clearAllTablesCompat()
            db.routineDao().upsertAll(file.routines)
            db.repeatRuleDao().upsertAll(file.rules)
            db.routineLogDao().upsertAll(file.logs)
            db.tagDao().upsertAll(file.tags)
            db.routineTagDao().upsertAll(file.routineTags)
            db.todoDao().upsertAll(file.todos)
        }
        changes.notifyChanged()
        file.summary()
    }

    companion object { const val SCHEMA_VERSION = 1 }
}

fun BackupFile.summary() = BackupSummary(
    routines = routines.count { it.deletedAt == null },
    logs = logs.count { it.deletedAt == null && (it.count > 0 || it.value != null) },
    tags = tags.count { it.deletedAt == null },
    todos = todos.count { it.deletedAt == null },
)

/** clearAllTables() 는 트랜잭션 안에서 호출할 수 없으므로 테이블별 DELETE. */
private fun AppDatabase.clearAllTablesCompat() {
    val w = openHelper.writableDatabase
    listOf("routines", "repeat_rules", "routine_logs", "tags", "routine_tags", "todos").forEach { w.execSQL("DELETE FROM $it") }
}
