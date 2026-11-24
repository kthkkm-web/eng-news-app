package app.myroutine.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.myroutine.MainActivity
import app.myroutine.core.newId
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.model.Todo
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.domain.repository.TagRepository
import app.myroutine.domain.repository.TodoRepository
import com.github.takahirom.roborazzi.captureRoboImage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime


/**
 * 실제 앱(Hilt, Room)을 Robolectric 에서 띄워 화면을 PNG 로 저장한다. 시각 확인용.
 * 실행: ./gradlew testDebugUnitTest -Pscreenshots --tests '*ScreenshotTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val out = "build/screenshots"

    @Before fun seed() {
        assumeTrue(System.getProperty("screenshots.enabled") == "true")
        val ep = AppEntryPoint.get(ApplicationProvider.getApplicationContext())
        runBlocking {
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
            ep.todos().saveTodo(Todo(newId(), "공과금 납부", today))
            ep.todos().saveTodo(Todo(newId(), "회의", today, LocalTime.of(14, 0)))
        }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        // DataStore/Room 은 실제 IO 스레드에서 돌기 때문에 잠깐 기다린다
        repeat(3) { Thread.sleep(150); compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        compose.onRoot().captureRoboImage("$out/$name.png")
    }

    @Test fun home_all_modes() {
        shot("01_home_card")
        compose.onNodeWithContentDescription("보기 모드 전환").performClick(); shot("02_home_grid")
        compose.onNodeWithContentDescription("보기 모드 전환").performClick(); shot("03_home_compact")
        compose.onNodeWithContentDescription("보기 모드 전환").performClick(); shot("04_home_check")
        compose.onNodeWithContentDescription("보기 모드 전환").performClick()
        compose.onNodeWithText("오늘의 할 일").performClick(); shot("05_home_todos")
        compose.onNodeWithContentDescription("그룹 설정").performClick(); shot("06_group_menu")
    }

    @Test fun add_and_edit() {
        compose.onNodeWithContentDescription("루틴 추가").performClick(); shot("10_add")
        compose.onNodeWithText("매주").performClick(); shot("11_add_weekly")
    }

    @Test fun edit_existing_and_color() {
        compose.onNodeWithText("뉴스 읽기", substring = true).performClick() // +1
        compose.onNodeWithContentDescription("메뉴", useUnmergedTree = true).let { }
        compose.onNodeWithContentDescription("루틴 추가").performClick()
        compose.onNodeWithContentDescription("뒤로").performClick()
        shot("12_after_tap")
    }
}
