package app.myroutine.domain.repository

import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.RoutineLog
import app.myroutine.domain.model.Tag
import app.myroutine.domain.model.Todo
import app.myroutine.domain.model.User
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 로그인 추상화. 지금은 LocalAuthRepository(로컬 사용자 1명) 만 존재. */
interface AuthRepository {
    val currentUser: Flow<User?>
    /** 동기 접근용(엔티티 userId 채우기). */
    fun currentUserId(): String
    suspend fun signIn(): Result<User>
    suspend fun signOut()
}

/** 서버 동기화 추상화. 지금은 no-op. */
interface SyncRepository {
    val isAvailable: Boolean
    suspend fun push(): Result<Unit>
    suspend fun pull(): Result<Unit>
}

interface RoutineRepository {
    /** 보관 포함, 삭제 제외. 규칙·태그 포함. */
    fun observeRoutines(): Flow<List<Routine>>
    suspend fun getRoutine(id: String): Routine?
    suspend fun saveRoutine(routine: Routine): String
    suspend fun deleteRoutine(id: String)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun reorder(idsInOrder: List<String>)

    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<RoutineLog>>
    suspend fun getLogs(from: LocalDate, to: LocalDate): List<RoutineLog>
    fun observeLogsOf(routineId: String): Flow<List<RoutineLog>>
    /** 횟수 변경(음수 허용). 0 미만으로는 내려가지 않음. */
    suspend fun addCount(routineId: String, date: LocalDate, delta: Int)
    suspend fun setCount(routineId: String, date: LocalDate, count: Int)
    suspend fun setValue(routineId: String, date: LocalDate, value: Double?)
    suspend fun resetLog(routineId: String, date: LocalDate)
}

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun saveTag(tag: Tag): String
    suspend fun deleteTag(id: String)
    suspend fun reorder(idsInOrder: List<String>)
    suspend fun seedDefaultsIfEmpty()
}

interface TodoRepository {
    fun observeTodos(date: LocalDate): Flow<List<Todo>>
    suspend fun getTodos(date: LocalDate): List<Todo>
    suspend fun saveTodo(todo: Todo): String
    suspend fun toggleDone(id: String)
    suspend fun deleteTodo(id: String)
}
