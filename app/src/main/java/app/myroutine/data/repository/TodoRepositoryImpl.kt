package app.myroutine.data.repository

import app.myroutine.core.DataChanges
import app.myroutine.core.TimeProvider
import app.myroutine.data.db.AppDatabase
import app.myroutine.data.db.TodoEntity
import app.myroutine.data.db.key
import app.myroutine.data.db.toDomain
import app.myroutine.domain.model.Todo
import app.myroutine.domain.repository.AuthRepository
import app.myroutine.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepositoryImpl @Inject constructor(
    db: AppDatabase,
    private val auth: AuthRepository,
    private val time: TimeProvider,
    private val changes: DataChanges,
) : TodoRepository {
    private val dao = db.todoDao()
    private val uid get() = auth.currentUserId()

    override fun observeTodos(date: LocalDate): Flow<List<Todo>> =
        dao.observeFor(uid, date.key()).map { l -> l.map { it.toDomain() } }

    override suspend fun getTodos(date: LocalDate): List<Todo> = dao.getFor(uid, date.key()).map { it.toDomain() }

    override suspend fun saveTodo(todo: Todo): String {
        val now = time.nowMillis()
        val existing = dao.get(todo.id)
        dao.upsert(
            TodoEntity(
                id = todo.id, userId = uid, title = todo.title.trim(), date = todo.date.key(),
                time = todo.time?.let { "%02d:%02d".format(it.hour, it.minute) }, done = todo.done,
                sortOrder = todo.sortOrder, createdAt = existing?.createdAt ?: now, updatedAt = now,
            ),
        )
        changes.notifyChanged()
        return todo.id
    }

    override suspend fun toggleDone(id: String) {
        val e = dao.get(id) ?: return
        dao.upsert(e.copy(done = !e.done, updatedAt = time.nowMillis()))
        changes.notifyChanged()
    }

    override suspend fun deleteTodo(id: String) {
        dao.softDelete(id, time.nowMillis())
        changes.notifyChanged()
    }
}
