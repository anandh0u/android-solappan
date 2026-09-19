import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun quotedBuildValue(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.solappan.agent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.solappan.agent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", quotedBuildValue(localProperties.getProperty("SUPABASE_URL", "")))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", quotedBuildValue(localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")))
        buildConfigField("boolean", "USE_GATEWAY", localProperties.getProperty("USE_GATEWAY", "false").toBoolean().toString())

        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            quotedBuildValue(if (localProperties.getProperty("USE_GATEWAY", "false").toBoolean()) "" else localProperties.getProperty("OPENAI_API_KEY", "")),
        )
        buildConfigField(
            "String",
            "OPENAI_MODEL",
            quotedBuildValue(localProperties.getProperty("OPENAI_MODEL", "")),
        )
        buildConfigField("int", "MAX_AGENT_TURNS", (localProperties.getProperty("MAX_AGENT_TURNS", "24").toIntOrNull()?.coerceIn(1, 40) ?: 24).toString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Never package the local developer credential in a distributable release APK.
    buildTypes {
        getByName("release") {
            buildConfigField("String", "OPENAI_API_KEY", quotedBuildValue(""))
            buildConfigField("boolean", "USE_GATEWAY", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("junit:junit:4.13.2")
}
