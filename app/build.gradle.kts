import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val signingProperties = Properties().apply {
    val config = rootProject.file("keystore.properties")
    if (config.isFile) config.inputStream().use { load(it) }
}
fun signingValue(property: String, environment: String): String? =
    signingProperties.getProperty(property) ?: providers.environmentVariable(environment).orNull
val uploadStore = signingValue("storeFile", "KEYSTORE_FILE")

android {
    namespace = "com.pegoku.ophaaldag"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pegoku.ophaaldag"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("versionCode").orElse("3").get().toInt()
        versionName = providers.gradleProperty("versionName").orElse("1.0.1").get()
        resourceConfigurations += listOf("en", "nl")
    }

    signingConfigs {
        if (uploadStore != null) {
            create("release") {
                storeFile = rootProject.file(uploadStore!!)
                storePassword = requireNotNull(signingValue("storePassword", "KEYSTORE_PASSWORD"))
                keyAlias = requireNotNull(signingValue("keyAlias", "KEY_ALIAS"))
                keyPassword = requireNotNull(signingValue("keyPassword", "KEY_PASSWORD"))
            }
        }
    }

    buildTypes {
        // Suffixed so a locally built debug APK installs alongside the Play/CI release build
        // instead of replacing it. The launcher and widget names are overridden in src/debug/res.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.work.runtime.ktx)
    implementation(libs.osmdroid)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16.1")
}
