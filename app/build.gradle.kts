plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val uploadKeystore = System.getenv("GHOSIUM_KEYSTORE_FILE")
val uploadStorePass = System.getenv("GHOSIUM_KEYSTORE_PASSWORD")
val uploadKeyAlias = System.getenv("GHOSIUM_KEY_ALIAS")
val uploadKeyPass = System.getenv("GHOSIUM_KEY_PASSWORD")
val hasUploadSigning = listOf(uploadKeystore, uploadStorePass, uploadKeyAlias, uploadKeyPass).all { !it.isNullOrBlank() }
android {
    namespace = "com.brendigo.ghosium"
    compileSdk = 36
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "com.brendigo.ghosium"
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = "0.1.4"
        val configuredUrl = providers.gradleProperty("GHOSIUM_SEARCH_BASE_URL")
            .orElse(providers.environmentVariable("GHOSIUM_SEARCH_BASE_URL")).orElse("").get()
        require(configuredUrl.isBlank() || (configuredUrl.startsWith("https://") && !configuredUrl.contains('"'))) {
            "GHOSIUM_SEARCH_BASE_URL must be an HTTPS base address"
        }
        buildConfigField("String", "GHOSIUM_SEARCH_BASE_URL", "\""+configuredUrl+"\"")
    }
    signingConfigs {
        if (hasUploadSigning) {
            create("upload") {
                storeFile = file(uploadKeystore!!)
                storePassword = uploadStorePass!!
                keyAlias = uploadKeyAlias!!
                keyPassword = uploadKeyPass!!
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("upload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.activity:activity:1.10.1")
    testImplementation("junit:junit:4.13.2")
}
