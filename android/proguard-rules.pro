# Kotlin metadata and generic signatures are used by Kotlin/Compose runtime inspection.
-keepattributes Signature,InnerClasses,EnclosingMethod

# Preserve the manifest-instantiated Android entry points by exact name.
-keep class vn.loi.learning.android.LearningEngineAndroidApplication { *; }
-keep class vn.loi.learning.android.MainActivity { *; }

# kotlinx.serialization generates serializers; its dependency-supplied consumer rules retain them.

# SQLDelight Android Driver
-keep class app.cash.sqldelight.** { *; }
-keep class vn.loi.learning.infrastructure.persistence.sqlite.** { *; }
