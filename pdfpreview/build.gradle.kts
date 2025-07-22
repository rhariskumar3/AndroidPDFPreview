import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.vanniktech.maven.publish.base")
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
    publishToMavenCentral(automaticRelease = true)
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