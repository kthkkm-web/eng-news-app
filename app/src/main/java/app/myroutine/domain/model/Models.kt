package app.myroutine.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay

/** 루틴 기록 방식. COUNT = 탭마다 +1, NUMERIC = 값 입력(몸무게 등) 시 그날 완료. */
enum class RecordType { COUNT, NUMERIC }

/** 반복 주기 유형. CUSTOM = 사용자가 고른 특정 날짜들. */
enum class Period { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

data class User(val id: String, val displayName: String, val isLocal: Boolean)

data class Routine(
    val id: String,
    val name: String,
    val emoji: String,
    /** "테마ID:인덱스" 형태의 색상 키. 실제 색은 ui.theme.RoutinePalettes 에서 해석. */
    val color: String,
    val recordType: RecordType = RecordType.COUNT,
    val unit: String? = null,
    val decimals: Int = 1,
    /** "목표치 설정" 토글. 꺼져 있으면 그날 목표는 항상 1회. */
    val goalEnabled: Boolean = true,
    val quickRecord: Boolean = false,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val templateKey: String? = null,
    val tagIds: List<String> = emptyList(),
    val rules: List<RepeatRule> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class RepeatRule(
    val id: String,
    val period: Period = Period.DAILY,
    /** WEEKLY 일 때 해당 요일. DAILY 에서는 무시. */
    val weekdays: Set<DayOfWeek> = emptySet(),
    /** WEEKLY + true 이면 "주중 아무 때나 target 회" (주 단위 목표). */
    val weeklyAnytime: Boolean = false,
    val monthDays: Set<Int> = emptySet(),
    val yearDates: Set<MonthDay> = emptySet(),
    val customDates: Set<LocalDate> = emptySet(),
    val timeEnabled: Boolean = false,
    val times: List<LocalTime> = emptyList(),
    val target: Int = 1,
    val notify: Boolean = false,
    /** 미리 알림(분). 0 = 없음. */
    val preNotifyMinutes: Int = 0,
) {
    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

data class RoutineLog(
    val id: String,
    val routineId: String,
    val date: LocalDate,
    val count: Int = 0,
    val value: Double? = null,
    val timestamps: List<Long> = emptyList(),
)

data class Tag(
    val id: String,
    val name: String,
    val emoji: String,
    val sortOrder: Int = 0,
)

data class Todo(
    val id: String,
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)
