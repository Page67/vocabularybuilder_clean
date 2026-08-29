plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val vocabularyContentDir = providers.gradleProperty("vocabularyContentDir")
    .map { rootProject.file(it) }
    .getOrElse(rootProject.file("content"))

require(vocabularyContentDir.isDirectory) {
    "Vocabulary content directory does not exist: $vocabularyContentDir"
}

android {
    namespace = "com.shiki.vocabulary"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.shiki.vocabulary"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main").assets.directories.add(vocabularyContentDir.absolutePath)
    }

    androidResources {
        ignoreAssetsPattern = "README.md:reviews:samples:schema"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
