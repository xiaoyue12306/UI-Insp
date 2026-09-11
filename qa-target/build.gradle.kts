plugins { id("com.android.application") }
android {
    namespace = "com.xiaoyue.inspectorfixture"
    compileSdk = 37
    defaultConfig { applicationId = "com.xiaoyue.inspectorfixture"; minSdk = 30; targetSdk = 37; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
