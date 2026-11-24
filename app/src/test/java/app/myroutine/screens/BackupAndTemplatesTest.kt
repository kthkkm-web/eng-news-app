package app.myroutine.screens

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.myroutine.MyRoutineApp
import app.myroutine.core.newId
import app.myroutine.data.backup.BackupManager
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.model.Routine
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.ui.templates.TemplateRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupAndTemplatesTest {
    private val app = ApplicationProvider.getApplicationContext<MyRoutineApp>()

    @Test fun backup_round_trip_replaces_data() = runBlocking {
        val ep = AppEntryPoint.get(app)
        ep.tags().seedDefaultsIfEmpty()
        val today = LocalDate.now()
        val r = Routine(newId(), "독서", "📚", "basic:0", startDate = today, rules = listOf(RepeatRule(newId(), Period.DAILY, target = 2)))
        ep.routines().saveRoutine(r)
        ep.routines().addCount(r.id, today, 1)

        val backup = AppEntryPoint.get(app).backup()
        val file = File(app.cacheDir, "b.json")
        val uri = Uri.fromFile(file)
        val sum = backup.exportTo(uri)
        assertEquals(1, sum.routines); assertEquals(1, sum.logs); assertEquals(7, sum.tags)

        // 백업 후 변경: 루틴 추가 + 기존 루틴 삭제
        ep.routines().saveRoutine(r.copy(id = newId(), name = "추가"))
        ep.routines().deleteRoutine(r.id)

        backup.restore(backup.read(uri))
        val after = ep.routines().observeRoutines().first()
        assertEquals(listOf("독서"), after.map { it.name })
        assertEquals(1, ep.routines().getLogs(today, today).first().count)
        assertEquals(7, ep.tags().observeTags().first().size)
    }

    @Test fun every_template_produces_a_valid_rule() {
        val repo = AppEntryPoint.get(app).templates()
        assertTrue(repo.categories.size >= 10)
        repo.categories.flatMap { it.items }.forEach { t ->
            val rule = repo.toRule(t)
            if (rule.period == Period.WEEKLY && !rule.weeklyAnytime) assertFalse("${t.key} 요일 없음", rule.weekdays.isEmpty())
            if (rule.period == Period.MONTHLY) assertFalse(t.key, rule.monthDays.isEmpty())
            assertTrue(t.key, RuleFormatter.ruleText(rule).isNotBlank())
        }
    }
}

