plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ir.khanoomi.geofencealert"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.khanoomi.geofencealert"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "1.9.1"
    }

    // امضای ثابت با کی‌استور ماندگار (GEOFENCE_DEBUG_KEYSTORE): دیباگ‌کی‌استور
    // پیش‌فرض گریدل در ~/.android است که با تعویض VM پاک می‌شد و هر بیلد امضای
    // متفاوتی می‌گرفت؛ در نتیجه نصب نسخه جدید روی نسخه قبلی ناممکن بود.
    signingConfigs {
        create("persistentDebug") {
            val ksPath = System.getenv("GEOFENCE_DEBUG_KEYSTORE")
                ?: "${System.getProperty("user.home")}/.android/debug.keystore"
            storeFile = file(ksPath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("persistentDebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
