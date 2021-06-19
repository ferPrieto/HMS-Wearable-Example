import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.huawei.agconnect")
    id("androidx.navigation.safeargs")
    id("kotlin-parcelize")
    id("kotlin-kapt")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    val peerPkgName: String = gradleLocalProperties(rootDir).getProperty("peerPkgName")
    val peerFingerPrint: String = gradleLocalProperties(rootDir).getProperty("peerFingerprint")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            buildConfigField("String", "peerPkgName", peerPkgName)
            buildConfigField("String", "peerFingerPrint", peerFingerPrint)
        }
    }

    val keyAliasProperty: String = gradleLocalProperties(rootDir).getProperty("keyAlias")
    val keyPasswordProperty: String = gradleLocalProperties(rootDir).getProperty("keyPassword")
    val storePasswordProperty: String = gradleLocalProperties(rootDir).getProperty("storePassword")

    signingConfigs {
        getByName("debug") {
            keyAlias = keyAliasProperty
            keyPassword = keyPasswordProperty
            storeFile = file("../keystore/debug.keystore")
            storePassword = storePasswordProperty
        }
    }
}

dependencies {
    fun kapt(definition: Any) = "kapt"(definition)
    fun implementation(definition: Any) = "implementation"(definition)
    fun testImplementation(definition: Any) = "testImplementation"(definition)
    fun androidTestImplementation(definition: Any) = "androidTestImplementation"(definition)

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

    kapt(Dependencies.Dagger.daggerCompiler)
    implementation(Dependencies.Dagger.daggerAndroid)
    implementation(Dependencies.Dagger.daggerAndroidSupport)
    kapt(Dependencies.Dagger.daggerAndroidProcessor)

    testImplementation(kotlin("test"))
    testImplementation(TestDependencies.livedataTesting)
    testImplementation(TestDependencies.kotlinxCoroutines)
    testImplementation(TestDependencies.JUnit.junit)
    testImplementation(TestDependencies.JUnit.junitPlatformRunner)
    testImplementation(TestDependencies.Mockito.mockitoCore)
    testImplementation(TestDependencies.Mockito.mockitoInline)
    testImplementation(TestDependencies.Mockito.mockitoKotlin)
    testImplementation(TestDependencies.AndroidX.coreTesting)
    testImplementation(TestDependencies.AndroidX.junit)
}
