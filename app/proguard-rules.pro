# Keep stack traces readable for production crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Entry points invoked by the framework.
-keep class com.rgbtv.app.MainActivity { *; }

# Models travel between the parser, the ViewModel and the UI; keep their shape stable.
-keep class com.rgbtv.app.model.** { *; }

# Media3 and Compose ship their own consumer keep rules, so nothing else is needed here.
# If a release build ever loses HLS playback, add:
# -keep class androidx.media3.exoplayer.hls.** { *; }
