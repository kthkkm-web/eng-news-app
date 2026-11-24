# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.myroutine.** { *** Companion; }
-keepclasseswithmembers class app.myroutine.** { kotlinx.serialization.KSerializer serializer(...); }
