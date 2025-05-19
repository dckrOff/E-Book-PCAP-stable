plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "uz.dckroff.pcap"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.dckroff.pcap"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
//    allprojects {
//        tasks.matching { it.name.contains("kapt") }.configureEach {
//            enabled = false
//        }
//    }

    // Настройка для использования как ui/, так и features/ директорий
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Architecture Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    kapt(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Dagger Hilt
    implementation(libs.dagger.hilt.android) { isTransitive = true }
    kapt(libs.dagger.hilt.compiler)

    // UI Components
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // Glide for image loading
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Logging
    implementation(libs.timber)

    // MPAndroidChart for progress visualization
    implementation(libs.mpandroidchart)

    // Gson для сериализации/десериализации JSON
    implementation(libs.gson)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // glide
//    implementation("com.github.bumptech.glide:glide:4.16.0")
//    implementation("com.squareup.okhttp3:okhttp:4.10.0")
//    implementation("com.github.bumptech.glide:okhttp3-integration:4.0.0") {
//        exclude("glide-parent")
//    }

    implementation("com.android.support:multidex:1.0.3")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.mockito.core)
}