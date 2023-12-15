import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("org.jreleaser") version "1.9.0"
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.harissk.pdfpreview"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "io.github.rhariskumar3"
            artifactId = "pdfpreview"
            version = "1.0.0-SNAPSHOT"

            pom {
                name.set("Android PDF Preview")
                description.set("AndroidPDFPreview is a lightweight and easy-to-use SDK that enables you to display and interact with PDF documents in your Android apps")
                url.set("https://github.com/rhariskumar3/AndroidPDFPreview")
                packaging = "aar"
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/rhariskumar3/AndroidPDFPreview.git")
                    developerConnection.set("scm:git:ssh://github.com/rhariskumar3/AndroidPDFPreview.git")
                    url.set("https://github.com/rhariskumar3/AndroidPDFPreview")
                    tag.set("HEAD")
                }
                developers {
                    developer {
                        id.set("harissk")
                        name.set("Haris")
                        email.set("r.hariskumar3@gmail.com")
                    }
                }
            }

            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    project {
        copyright.set("Haris")
    }
    gitRootSearch.set(true)
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            nexus2 {
                create("maven-central") {
                    active.set(Active.ALWAYS)
                    url.set("https://s01.oss.sonatype.org/service/local")
                    snapshotUrl.set("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    closeRepository.set(false)
                    releaseRepository.set(false)
                    stagingRepositories.add("build/staging-deploy")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    api(project(":pdfium"))
}