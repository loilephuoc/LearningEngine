-keep enum vn.loi.learning.desktop.runtime.DesktopLogLevel {
    *;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class javazoom.spi.mpeg.sampled.** {
    *;
}

-keep class javazoom.jl.** {
    *;
}

-keep class org.tritonus.** {
    *;
}

-keep class * extends javax.sound.sampled.spi.AudioFileReader {
    *;
}

-keep class * extends javax.sound.sampled.spi.AudioFileWriter {
    *;
}

-keep class * extends javax.sound.sampled.spi.FormatConversionProvider {
    *;
}

-keep class * extends javax.sound.sampled.spi.MixerProvider {
    *;
}

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod


# Workaround for JVM VerifyError in StatisticsFacade.loadUiState().
# Keep this class bytecode untouched by ProGuard optimization.
-keep class vn.loi.learning.desktop.ui.statistics.StatisticsFacade {
    *;
}

# SQLite JDBC / SQLDelight
-keep class org.sqlite.** { *; }
-keep class app.cash.sqldelight.** { *; }
-keep class vn.loi.learning.infrastructure.persistence.sqlite.** { *; }