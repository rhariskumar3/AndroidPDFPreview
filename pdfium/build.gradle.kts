import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.vanniktech.maven.publish.base")
}

android {
    namespace = "com.harissk.pdfium"
    compileSdk = (property("compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("minSdk") as String).toInt()

        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    buildTypes {
        debug {
            isJniDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    sourceSets {
        getByName("main").java.srcDirs("src/main/cpp/pdfium/lib")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary())
    pomFromGradleProperties()
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
    coordinates(
        groupId = project.property("POM_GROUP_ID").toString(),
        artifactId = project.property("POM_ARTIFACT_ID").toString(),
        version = project.property("POM_VERSION").toString(),
    )
}