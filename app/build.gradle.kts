plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "bootanimation.zip.bootzipcreator"
    compileSdk = 36

    defaultConfig {
        applicationId = "bootanimation.zip.bootzipcreator"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    implementation(files("libs/mobile-ffmpeg-full-gpl-4.4.LTS.aar"))
    implementation(libs.smart.exception.java)
    implementation(libs.glide)
    implementation(libs.swiperefreshlayout)
    implementation(libs.app.update)
}