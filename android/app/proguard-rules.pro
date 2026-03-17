# Retrofit + OkHttp
-dontwarn okhttp3.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class in.kanoonsaathi.app.data.api.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# App models — keep only API/data layer for Gson; let R8 obfuscate all other classes
# The wildcard keep has been intentionally removed to allow full obfuscation of UI/VM/logic.
