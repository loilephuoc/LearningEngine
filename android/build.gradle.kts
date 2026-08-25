import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "vn.loi.learning.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "vn.loi.learning.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) {
                FileInputStream(file).use { load(it) }
            }
        }
        val supabaseUrl = providers.gradleProperty("LEARNING_ENGINE_SUPABASE_URL")
            .orElse(providers.environmentVariable("LEARNING_ENGINE_SUPABASE_URL"))
            .orNull?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty("LEARNING_ENGINE_SUPABASE_URL").orEmpty()
        val supabaseKey = providers.gradleProperty("LEARNING_ENGINE_SUPABASE_PUBLISHABLE_KEY")
            .orElse(providers.environmentVariable("LEARNING_ENGINE_SUPABASE_PUBLISHABLE_KEY"))
            .orNull?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty("LEARNING_ENGINE_SUPABASE_PUBLISHABLE_KEY").orEmpty()
        fun quoted(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        buildConfigField("String", "FAMILY_SUPABASE_URL", quoted(supabaseUrl))
        buildConfigField("String", "FAMILY_SUPABASE_PUBLISHABLE_KEY", quoted(supabaseKey))
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":"))
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
