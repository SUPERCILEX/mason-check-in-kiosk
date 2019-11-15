# Keep line numbers and file name obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-printconfiguration build/outputs/mapping/configuration.txt
-dontobfuscate

# For screen analytics
-keepnames class * extends com.bymason.kiosk.checkin.core.ui.FragmentBase
-keepnames class * extends com.bymason.kiosk.checkin.core.ui.DialogFragmentBase

# Don't touch Gson fields
-keep @interface com.google.gson.annotations.SerializedName
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName *;
}

# 🤷‍♂️
-dontwarn org.apache.commons.**
-dontnote kotlin.**
-dontnote com.google.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
