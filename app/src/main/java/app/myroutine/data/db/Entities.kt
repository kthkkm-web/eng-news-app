package app.myroutine.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * 동기화 대비 공통 필드: id(UUID 문자열), userId, createdAt, updatedAt, deletedAt(soft delete).
 * 날짜는 "yyyy-MM-dd", 시각은 "HH:mm" 문자열로 저장한다(정렬 가능 + 서버 JSON 과 호환).
 * FK 는 걸지 않는다: soft delete / 서버 동기화 순서와 충돌하지 않게 하기 위함.
 */

@Serializable
@Entity(tableName = "routines", indices = [Index("userId")])
data class RoutineEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val emoji: String,
    val color: String,
    val recordType: String,
    val unit: String?,
    val decimals: Int,
    val goalEnabled: Boolean,
    val quickRecord: Boolean,
    val startDate: String,
    val endDate: String?,
    val sortOrder: Int,
    val archived: Boolean,
    val templateKey: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
@Entity(tableName = "repeat_rules", indices = [Index("routineId")])
data class RepeatRuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val routineId: String,
    val sortOrder: Int,
    val period: String,
    /** 비트마스크: 월=1<<0 ... 일=1<<6 (DayOfWeek.value-1) */
    val weekdays: Int,
    val weeklyAnytime: Boolean,
    val monthDays: List<Int>,
    /** "MM-dd" */
    val yearDates: List<String>,
    /** "yyyy-MM-dd" */
    val customDates: List<String>,
    val timeEnabled: Boolean,
    /** "HH:mm" */
    val times: List<String>,
    val target: Int,
    val notify: Boolean,
    val preNotifyMinutes: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** (routineId, date) 당 1행. 기록 취소는 행 삭제가 아니라 count=0/value=null 로 갱신한다. */
@Serializable
@Entity(
    tableName = "routine_logs",
    indices = [Index(value = ["routineId", "date"], unique = true), Index("date")],
)
data class RoutineLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val routineId: String,
    val date: String,
    val count: Int,
    val value: Double?,
    val timestamps: List<Long>,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
@Entity(tableName = "tags", indices = [Index("userId")])
data class TagEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val emoji: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
@Entity(
    tableName = "routine_tags",
    indices = [Index(value = ["routineId", "tagId"], unique = true), Index("tagId")],
)
data class RoutineTagEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val routineId: String,
    val tagId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
@Entity(tableName = "todos", indices = [Index("date")])
data class TodoEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val date: String,
    val time: String?,
    val done: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
