import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "cvam.dignity.bhumess"

    compileSdk = 36

    // Required by AndroidX PDF alpha18
    compileSdkExtension = 19

    defaultConfig {
        applicationId = "cvam.dignity.bhumess"

        minSdk = 28
        targetSdk = 35

        versionCode = 40
        versionName = "1.1.1.40"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // NDK r28+ for 16 KB page-size support
        ndkVersion = "28.0.12433566"

        externalNativeBuild {
            cmake {
                cppFlags(
                    "-Wl,-z,max-page-size=16384"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
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

tasks.withType<
        org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        >().configureEach {
    compilerOptions {
        jvmTarget.set(
            JvmTarget.JVM_11
        )
    }
}

dependencies {

    // =========================================================
    // Core Android
    // =========================================================

    implementation(libs.androidx.core.ktx)

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.7"
    )

    implementation(
        libs.androidx.activity.compose
    )


    // =========================================================
    // Jetpack Compose
    // =========================================================

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.foundation
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    // =========================================================
    // PDF VIEWER
    // =========================================================

    implementation(
        "androidx.pdf:pdf-viewer-fragment:1.0.0-alpha18"
    )


    // =========================================================
    // Google Mobile Ads
    // =========================================================

    implementation(
        "com.google.android.gms:play-services-ads:25.4.0"
    )


    // =========================================================
    // Google Play In-App Updates
    // =========================================================

    implementation(
        "com.google.android.play:app-update:2.1.0"
    )

    implementation(
        "com.google.android.play:app-update-ktx:2.1.0"
    )


    // =========================================================
    // Networking & HTML Parsing
    // =========================================================

    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )

    implementation(
        "org.jsoup:jsoup:1.17.2"
    )


    // =========================================================
    // PDF Text Extraction
    // =========================================================

    implementation(
        "com.itextpdf:itextg:5.5.10"
    )

    implementation(
        "io.github.grizzi91:bouquet:1.1.2"
    )


    // =========================================================
    // Google Drive API
    // =========================================================

    implementation(
        "com.google.api-client:google-api-client-android:2.2.0"
    )

    implementation(
        "com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0"
    )

    implementation(
        "com.google.auth:google-auth-library-oauth2-http:1.23.0"
    )


    // =========================================================
    // Testing
    // =========================================================

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}