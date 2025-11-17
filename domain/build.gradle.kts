import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Dagger 2
    implementation(libs.dagger)

    testImplementation(libs.junit)
}

