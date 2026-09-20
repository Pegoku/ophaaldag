# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.pegoku.ophaaldag.**$$serializer { *; }
-keepclassmembers class com.pegoku.ophaaldag.** { *** Companion; }
-keepclasseswithmembers class com.pegoku.ophaaldag.** { kotlinx.serialization.KSerializer serializer(...); }
# OkHttp
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Room
# work-runtime 2.7.1 pins room-runtime 2.2.5, whose consumer rule is a bare
# "-keep class * extends androidx.room.RoomDatabase". Under R8 full mode that keeps the
# class but not its default constructor, so Room's Class.forName(...).newInstance() on
# WorkDatabase_Impl throws InstantiationException at startup. Room 2.3.0+ ships this form.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
