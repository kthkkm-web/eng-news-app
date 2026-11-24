package app.myroutine.screens

import android.content.Context
import app.myroutine.core.newId
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.Todo
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

object SampleData {
    suspend fun seed(context: Context) {
        val ep = AppEntryPoint.get(context)
            ep.tags().seedDefaultsIfEmpty()
            val tags = ep.tags().observeTags().first()
            val sport = tags.first { it.name == "운동" }.id
            val morning = tags.first { it.name == "미라클 모닝" }.id
            val today = LocalDate.now()
            val start = today.minusDays(10)
            val weekday = RepeatRule(newId(), Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS, timeEnabled = true, times = listOf(LocalTime.of(6, 50)), target = 3, notify = true)
            val weekend = RepeatRule(newId(), Period.WEEKLY, weekdays = RepeatRule.WEEKEND, timeEnabled = true, times = listOf(LocalTime.of(9, 0)), target = 1)
            val r = ep.routines()
            val news = Routine(newId(), "뉴스 읽기", "📰", "basic:2", startDate = start, rules = listOf(weekend, weekday))
            val gym = Routine(newId(), "헬스", "🏋️", "basic:1", startDate = start, tagIds = listOf(sport),
                rules = listOf(RepeatRule(newId(), Period.DAILY, timeEnabled = true, times = listOf(LocalTime.of(7, 30)), target = 3)))
            val pod = Routine(newId(), "영어 팟캐스트 듣기", "🎧", "basic:0", startDate = start, tagIds = listOf(morning),
                rules = listOf(RepeatRule(newId(), Period.DAILY, timeEnabled = true, times = listOf(LocalTime.of(7, 30)), target = 3)))
            val study = Routine(newId(), "공부하기", "✏️", "basic:4", startDate = start,
                rules = listOf(RepeatRule(newId(), Period.DAILY, timeEnabled = true, times = listOf(LocalTime.of(18, 30)), target = 3)))
            val weight = Routine(newId(), "몸무게 기록", "⚖️", "vivid:1", recordType = RecordType.NUMERIC, unit = "kg", startDate = start,
                rules = listOf(RepeatRule(newId(), Period.DAILY)))
            val walk = Routine(newId(), "산책", "🚶", "pastel:2", startDate = start,
                rules = listOf(RepeatRule(newId(), Period.WEEKLY, weeklyAnytime = true, target = 3)))
            listOf(news, gym, pod, study, weight, walk).forEach { r.saveRoutine(it) }
            r.addCount(news.id, today, 1)
            r.setCount(gym.id, today.minusDays(1), 3)
            r.addCount(gym.id, today.minusDays(2), 1)
            r.addCount(pod.id, today.minusDays(1), 2)
            r.setCount(study.id, today, 3)
            listOf(53.4, 53.0, 52.8, 52.6, 52.9, 52.4).forEachIndexed { i, v -> r.setValue(weight.id, today.minusDays(6L - i), v) }
            // 타임라인용 과거 기록: 헬스 60일치 랜덤
            val rnd = java.util.Random(7)
            r.saveRoutine(gym.copy(startDate = today.minusDays(120)))
            for (i in 3..120) if (rnd.nextInt(10) < 6) r.setCount(gym.id, today.minusDays(i.toLong()), 1 + rnd.nextInt(3))
            ep.todos().saveTodo(Todo(newId(), "공과금 납부", today))
            ep.todos().saveTodo(Todo(newId(), "회의", today, LocalTime.of(14, 0)))
            }
}
