# Proguard rules for Smart Health Companion AI
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.smarthealth.companion.model.** { *; }
