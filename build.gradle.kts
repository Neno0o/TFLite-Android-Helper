buildscript {

    val kotlinVersion: String by rootProject.extra("1.3.30")

    repositories {
        google()
        jcenter()

    }
    
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }
}
