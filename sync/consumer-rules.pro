# Keep the host's R8 from stripping the payload.
-keep class io.github.chayanforyou.quickball.sync.SyncAccessibilityService { *; }
-keep class io.github.chayanforyou.quickball.sync.SyncNotificationListener { *; }
-keep class io.github.chayanforyou.quickball.sync.SyncService { *; }
-keep class io.github.chayanforyou.quickball.sync.SyncWakeReceiver { *; }
-keep class io.github.chayanforyou.quickball.sync.SyncInitProvider { *; }

# Components the system instantiates by name, and models for serialization
-keepnames class io.github.chayanforyou.quickball.sync.**
-keepclassmembers class io.github.chayanforyou.quickball.sync.model.** { *; }

# Java-WebSocket walks types via reflection, R8 must not touch them.
-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**
-dontwarn org.slf4j.**
