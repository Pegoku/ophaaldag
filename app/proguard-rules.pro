# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.pegoku.curem3.**$$serializer { *; }
-keepclassmembers class com.pegoku.curem3.** { *** Companion; }
-keepclasseswithmembers class com.pegoku.curem3.** { kotlinx.serialization.KSerializer serializer(...); }
# OkHttp
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
