plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.huawei.agconnect")
    id("androidx.navigation.safeargs")
    id("kotlin-parcelize")
}

android {
    defaultConfig {
        applicationId = "com.fprieto.hms.wearable"
        minSdkVersion(AndroidSettings.minSdk)
        targetSdkVersion(AndroidSettings.targetSdk)
        versionCode = AndroidSettings.appVersionCode
        versionName = AndroidSettings.appVersionName
    }
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion(AndroidSettings.buildTools)
    compileSdkVersion(AndroidSettings.compileSdk)
    kotlinOptions{
        jvmTarget = "1.8"
    }
    compileOptions{
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }
    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
        }
    }
}

dependencies {
    implementation(Dependencies.AndroidX.fragmentKtx)
    implementation(Dependencies.AndroidX.lifecycleLivedataKtx)
    annotationProcessor(Dependencies.AndroidX.lifecycleCompiler)
    implementation(Dependencies.AndroidX.archComponents)
    implementation(Dependencies.AndroidX.coreKtx)
    implementation(Dependencies.AndroidX.appCompat)
    implementation(Dependencies.AndroidX.lifeCycleCommon)

    implementation(Dependencies.AndroidX.constraintlayout)
    implementation(Dependencies.AndroidX.Navigation.fragmentKtx)
    implementation(Dependencies.AndroidX.Navigation.uiKtx)

    implementation(Dependencies.Kotlin.jdk8)
    implementation(Dependencies.Kotlin.coroutines)

    implementation(Dependencies.Huawei.wearengine)
    implementation(Dependencies.Huawei.hmsBase)

    implementation(Dependencies.ExoPlayer.core)
    implementation(Dependencies.ExoPlayer.ui)

    implementation(Dependencies.timber)

    implementation(Dependencies.gson)
}
