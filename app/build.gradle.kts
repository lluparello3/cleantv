plugins {
    id("com.android.application")
}

android {
    namespace = "com.cleantv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cleantv.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "2.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
