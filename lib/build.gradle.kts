/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin library project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/6.7.1/userguide/building_java_projects.html
 */
val letsGoooDatastoreVersion = "1.1.0";

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"

    `java-library`
}

repositories {
    // Use JCenter for resolving dependencies.
    google()
    jcenter()
}

kotlin {
    sourceSets {
        main.configure {
            kotlin.srcDirs( "src/main/kotlin")
        }
        test.configure {
            kotlin.srcDirs("src/test/kotlin")
        }
    }
}

java {
    sourceSets {
        main.configure {
            java.srcDirs("src/main/kotlin")
        }
        test.configure {
            java.srcDirs("src/test/kotlin")
        }
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.google.code.gson:gson:2.8.7")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    testImplementation("org.mockito:mockito-core:3.12.4")

}
