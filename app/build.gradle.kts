import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.reyzie.hymns"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.reyzie.hymns"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "POSTHOG_API_KEY", "\"${properties.getProperty("POSTHOG_API_KEY", "")}\"")
        buildConfigField("String", "POSTHOG_HOST", "\"${properties.getProperty("POSTHOG_HOST", "https://us.i.posthog.com")}\"")
        
        buildConfigField("String", "JIRA_URL", "\"${properties.getProperty("JIRA_URL", "")}\"")
        buildConfigField("String", "JIRA_EMAIL", "\"${properties.getProperty("JIRA_EMAIL", "")}\"")
        buildConfigField("String", "JIRA_API_TOKEN", "\"${properties.getProperty("JIRA_API_TOKEN", "")}\"")
        buildConfigField("String", "JIRA_PROJECT_KEY", "\"${properties.getProperty("JIRA_PROJECT_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    // Pin above BOM — 1.4.x hides expressive APIs; 1.5.0-alpha exposes ButtonGroup, FloatingToolbar, etc.
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.squareup.okhttp3)
    implementation(libs.google.gson)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.compose.auth)
    implementation(libs.supabase.compose.auth.ui)
    implementation(libs.supabase.storage)
    implementation(libs.play.services.cast.framework)
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("com.posthog:posthog-android:3.+")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    // Material 3 expressive components (FloatingToolbar, ButtonGroup, ToggleButton) still
    // require opt-in on 1.5.0-alpha; the BOM pins stable 1.4.x which hides these APIs.
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}