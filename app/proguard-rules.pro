# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# RxJava + RxAndroid
-keep class io.reactivex.** { *; }
-keep class io.reactivex.android.** { *; }

# Doki + Markwon
-keep class dev.doubledot.doki.** { *; }
-keep class ru.noties.markwon.** { *; }
-keep class ru.noties.markwon.image.** { *; }

