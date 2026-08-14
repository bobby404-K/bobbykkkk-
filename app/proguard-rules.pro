# ProGuard rules for SafeWord

# Keep Room database schemas and models
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# JNA requires these to avoid warnings regarding AWT when compiling for Android
-dontwarn java.awt.**
-dontwarn javax.naming.**
