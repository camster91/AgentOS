plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("multiplatform") version "1.9.23"
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("maven-publish")
}

group = "com.agentOS"
version = "0.1.0"

repositories {
    google()
    mavenCentral()
}

subprojects {
    repositories {
        google()
        mavenCentral()
    }
}
