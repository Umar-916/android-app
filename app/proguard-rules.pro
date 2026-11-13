# ORBIT Planner release rules.
# Keep line numbers so Play Console crash traces stay readable (no private data is ever logged).
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,Signature,EnclosingMethod

# --- kotlinx.serialization: navigation keys and backup DTOs are (de)serialised by generated serializers -------
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.funsol.orbit.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.funsol.orbit.core.navigation.AppDestination { *; }
-keep class com.funsol.orbit.core.navigation.AppDestination$* { *; }
# Backup file format: field names ARE the file format, never rename them.
-keep class com.funsol.orbit.core.backup.BackupFile { *; }
-keep class com.funsol.orbit.core.database.entity.** { *; }

# --- WorkManager workers and broadcast receivers are instantiated by name ----------------------------------
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class com.funsol.orbit.core.notifications.** { *; }

# --- Koin resolves ViewModels through Kotlin reflection-free DSL; keep ViewModel constructors for safety ----
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Drop verbose logging in release.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# --- Ads / consent / Remote Config --------------------------------------------------------------------------------
# The Google Mobile Ads Next-Gen SDK, UMP and Firebase Remote Config ship their own consumer ProGuard rules, so
# nothing is kept here on purpose. ORBIT's own ad code uses no reflection (AdConfig and the onboarding flow JSON
# are parsed by hand), so no extra keep rules are required for com.funsol.orbit.core.ads / remote / consent.
