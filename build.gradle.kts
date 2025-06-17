import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.gradle.maven.publish.plugin)
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinKapt) apply false
    alias(libs.plugins.dokka) apply false
//    alias(libs.plugins.kotlinCompose) apply false
}

allprojects {
    // Necessary to publish to Maven.
    group = property("POM_GROUP_ID") as String
    version = property("POM_VERSION") as String

    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTaskPartial>().configureEach {
        println("DokkaTaskPartial $name ${moduleName.orNull}")
        pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.base.DokkaBase" to """{ "separateInheritedMembers": true }"""))
        dokkaSourceSets.configureEach {
            jdkVersion.set(17)
            languageVersion.set(libs.versions.kotlin.get())
            suppressInheritedMembers.set(true)
            suppressObviousFunctions.set(true)
            if (moduleName.orNull?.contains("app") == true)
                documentedVisibilities.set(emptyList())
        }
    }
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    println("DokkaMultiModuleTask $name ${moduleName.orNull}")
}