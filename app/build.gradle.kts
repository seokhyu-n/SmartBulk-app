import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // ✅ Firebase 연동
    kotlin("kapt")
}

android {
    namespace = "com.example.smartbulk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartbulk"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = false
        dataBinding = true   // ✅ DataBinding 활성화
        viewBinding = true   // ✅ ViewBinding 활성화
    }
}

dependencies {
    // ✅ Firebase BOM (버전 관리)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // ✅ Firebase 인증
    implementation("com.google.firebase:firebase-auth-ktx")

    // ✅ Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    // ✅ Firebase Cloud Functions (AI 식단 추천 — Anthropic API 키를 서버에서만 사용하기 위한 프록시)
    implementation("com.google.firebase:firebase-functions-ktx")

    // ✅ 앱 기본 구성 요소 (XML 기반)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ Glide - GIF 및 이미지 로딩 라이브러리
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // ✅ 테스트 라이브러리
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ✅ Flexbox 레이아웃
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // ------------------------------------------------
    // ⭐ TensorFlow Lite 추가
    // ------------------------------------------------
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.12.0")

    // ------------------------------------------------
    // ⭐ CameraX 추가 (Kotlin DSL 문법)
    // ------------------------------------------------
    val camerax_version = "1.2.3"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("androidx.camera:camera-extensions:$camerax_version")
}
