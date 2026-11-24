# EventKampus ProGuard Rules

# ===== Model classes (Serializable) =====
-keep class com.example.eventkampus.data.model.** { *; }

# ===== SQLite / Database Helper =====
-keep class com.example.eventkampus.data.db.** { *; }

# ===== Repository & Session =====
-keep class com.example.eventkampus.data.repository.** { *; }
-keep class com.example.eventkampus.data.session.** { *; }

# ===== ViewBinding (tidak perlu di-obfuscate) =====
-keep class com.example.eventkampus.databinding.** { *; }

# ===== Material Design & AndroidX =====
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }

# ===== WebView JavaScript interface =====
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ===== JSON org.json =====
-keep class org.json.** { *; }

# ===== Kotlin Serialization & Coroutines =====
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ===== Remove logging dari release =====
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
