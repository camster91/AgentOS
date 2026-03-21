plugins {
    id("com.android.application") version "8.5.0"
    kotlin("android") version "1.9.23"
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.agentOS.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agentOS.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:compose-ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:compose-ui-tooling-preview")
    debugImplementation("androidx.compose.ui:compose-ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
}
