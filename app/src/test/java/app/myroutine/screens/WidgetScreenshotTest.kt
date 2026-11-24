package app.myroutine.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import app.myroutine.widget.DataWidget
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.myroutine.MyRoutineApp
import app.myroutine.widget.WidgetUpdater
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 위젯 RemoteViews 를 실제 View 로 펼쳐 PNG 로 저장. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
class WidgetScreenshotTest {

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test fun widgets() {
        assumeTrue(System.getProperty("screenshots.enabled") == "true")
        val app = ApplicationProvider.getApplicationContext<MyRoutineApp>()
        runBlocking { SampleData.seed(app) }
        val outDir = File("build/screenshots").apply { mkdirs() }
        WidgetUpdater.all.forEachIndexed { i, widget ->
            val small = i < 3
            val size = if (small) DpSize(170.dp, 170.dp) else DpSize(350.dp, 170.dp)
            @Suppress("UNCHECKED_CAST")
            val dw = widget as DataWidget<Any?>
            val rv = runBlocking {
                val data = dw.load(app, null)
                GlanceRemoteViews().compose(app, size, state = emptyPreferences()) { dw.Content(app, data) }.remoteViews
            }
            val parent = FrameLayout(app)
            val view: View = rv.apply(app, parent)
            val d = app.resources.displayMetrics.density
            val w = (size.width.value * d).toInt(); val h = (size.height.value * d).toInt()
            parent.addView(view, FrameLayout.LayoutParams(w, h))
            parent.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            parent.layout(0, 0, w, h)
            val bmp = Bitmap.createBitmap(w + 40, h + 40, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp); c.drawColor(0xFF3A3F45.toInt()); c.translate(20f, 20f)
            parent.draw(c)
            File(outDir, "w%d_%s.png".format(i + 1, widget.javaClass.simpleName)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
