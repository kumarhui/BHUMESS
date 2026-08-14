import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "cvam.dignity.bhumess"
    compileSdk = 35

    defaultConfig {
        applicationId = "cvam.dignity.bhumess"
        minSdk = 24
        targetSdk = 35
        versionCode = 37
        versionName = "1.1.1.37"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // CRITICAL: Use NDK r28 or higher to ensure 16 KB alignment support
        ndkVersion = "28.0.12433566"

        externalNativeBuild {
            cmake {
                // Forces the linker to use 16 KB boundaries for any custom C++ code
                cppFlags("-Wl,-z,max-page-size=16384")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // Setting this to false allows the Play Store to serve uncompressed, 16 KB aligned libs.
            useLegacyPackaging = false
        }
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/*.kotlin_module"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// FIX for Cloudinary/Fresco 16 KB issues
configurations.all {
    resolutionStrategy {
        // Force Fresco versions that are 16 KB compliant (v3.4.0+)
        force("com.facebook.fresco:fresco:3.4.0")
        force("com.facebook.soloader:soloader:0.12.1")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {

    // Core Compose & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)

    // UI & Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("com.google.android.gms:play-services-ads:25.4.0")

    // Networking & HTML Parsing
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")

    // PDF Text Extraction (iTextG)
    implementation("com.itextpdf:itextg:5.5.10")

    // Drive API
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}