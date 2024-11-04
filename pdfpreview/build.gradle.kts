import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.vanniktech.maven.publish.base")
}

kotlin {
    jvmToolchain(11)
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

android {
    namespace = "com.harissk.pdfpreview"
    compileSdk = (property("compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("minSdk") as String).toInt()

        consumerProguardFiles("consumer-rules.pro")
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
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary())
    pomFromGradleProperties()
    publishToMavenCentral(host = SonatypeHost.S01, automaticRelease = true)
    signAllPublications()
    coordinates(
        groupId = project.property("POM_GROUP_ID").toString(),
        artifactId = project.property("POM_ARTIFACT_ID").toString(),
        version = project.property("POM_VERSION").toString(),
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    api(project(":pdfium"))
}