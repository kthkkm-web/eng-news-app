package app.myroutine.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
        runBlocking { SampleData.seed(ApplicationProvider.getApplicationContext()) }
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

    @Test fun timeline_and_detail() {
        compose.onNodeWithText("타임라인").performClick(); shot("20_timeline_1m")
        compose.onNodeWithText("1M").performClick(); shot("21_timeline_1y")
        compose.onNodeWithText("1Y").performClick(); shot("22_timeline_1w")
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("몸무게", substring = true))
        compose.onNodeWithText("몸무게", substring = true).performClick(); shot("23_detail_numeric")
    }

    @Test fun phase7_screens() {
        compose.onNodeWithText("추천루틴").performClick(); shot("30_templates")
        compose.onNodeWithText("물 2L 마시기", substring = true, useUnmergedTree = true).let { compose.onAllNodesWithText("물 2L 마시기", substring = true)[0].performClick() }
        shot("31_edit_from_template")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("내정보").performClick(); shot("32_profile")
        compose.onNodeWithText("알림 설정").performClick(); shot("33_notifications")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("데이터 백업/복원").performClick(); shot("34_backup")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("루틴 및 할 일 설정").performClick()
        compose.onNodeWithText("태그 관리").performClick(); shot("35_tags")
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("위젯 가이드").performClick(); shot("36_widget_guide")
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
