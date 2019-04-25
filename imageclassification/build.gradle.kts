import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
}

val tfliteVersion: String by rootProject.extra("1.13.1")

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(15)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test:runner:1.1.1")
    implementation("org.tensorflow:tensorflow-lite:$tfliteVersion")
}
