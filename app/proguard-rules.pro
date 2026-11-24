# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.myroutine.** { *** Companion; }
-keepclasseswithmembers class app.myroutine.** { kotlinx.serialization.KSerializer serializer(...); }

# Glance: ActionCallback 은 클래스 이름으로 리플렉션 생성된다
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { <init>(); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { <init>(); }

# WorkManager Worker (리플렉션 생성)
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
