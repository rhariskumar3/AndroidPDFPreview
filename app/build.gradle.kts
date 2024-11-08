plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinKapt)
}

kotlin {
    jvmToolchain(17)
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.harissk.androidpdfpreview"
    compileSdk = (property("compileSdk") as String).toInt()

    defaultConfig {
        applicationId = "com.harissk.androidpdfpreview"
        minSdk = (property("minSdk") as String).toInt()
        targetSdk = (property("compileSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = true
        }
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    implementation(project(":pdfpreview"))
}