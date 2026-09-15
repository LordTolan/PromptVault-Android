plugins { id("com.android.application") }

android {
    namespace = "com.lordtolan.promptvault"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.lordtolan.promptvault"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
}
