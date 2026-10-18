plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val releaseKeystoreFile: String? =
    findProperty("RELEASE_KEYSTORE_FILE")?.toString()
        ?: System.getenv("RELEASE_KEYSTORE_FILE")
val releaseKeystorePassword: String? =
    findProperty("RELEASE_KEYSTORE_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? =
    findProperty("RELEASE_KEY_ALIAS")?.toString()
        ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? =
    findProperty("RELEASE_KEY_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEY_PASSWORD")

val hasSigningConfig = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).none { it.isNullOrBlank() }

android {
    namespace = "fr.mandarine.environmentquality"
    // Compiled against 37 because compose-ui 1.12.x (Compose BOM 2026.09.00)
    // refuses to be consumed by anything older. targetSdk stays at 36: compiling
    // against a newer SDK is not the same as opting in to its runtime behaviour.
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "fr.mandarine.environmentquality"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("test").assets.srcDir("$projectDir/schemas")
    }

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Sampling dates are java.time, which arrived in API 26. minSdk is 24, so
        // the library is backported into the dex rather than taken from the platform.
        isCoreLibraryDesugaringEnabled = true
    }
    lint {
        // NewApi is what catches java.time running under minSdk 24; it stays fatal
        // so the floor can never silently drop.
        abortOnError = true
        checkReleaseBuilds = true
        fatal += "NewApi"
        warningsAsErrors = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    testImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Not imported by any test. It is declared to pull espresso up to the version
    // in the catalogue: compose ui-test-junit4 drags in 3.5.0 transitively, and
    // dropping this line silently downgrades it.
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
