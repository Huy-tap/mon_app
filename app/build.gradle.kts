plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.fitnessapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.fitnessapp"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.fitnessapp.IsolatedRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
