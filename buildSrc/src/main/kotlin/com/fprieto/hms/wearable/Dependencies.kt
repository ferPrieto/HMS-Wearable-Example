object AndroidSettings {
    const val appVersionName = "0.1"
    const val appVersionCode = 1
    const val compileSdk = 30
    const val buildTools = "30.0.2"
    const val minSdk = 23
    const val targetSdk = 30
}

object Versions {
    const val appCompat = "1.2.0"
    const val navigation = "2.3.0"
    const val constraintLayout = "2.0.4"
    const val fragmentKtx = "1.3.2"
    const val lifecycleLivedataKtx = "2.3.1"
    const val gradle = "3.5.1"
    const val safeArgs = "2.3.5"
    const val kotlinxCoroutines = "1.4.2"
    const val kotlin = "1.4.0"
    const val agcp = "1.4.2.300"
    const val wearEngine = "5.0.1.301"
    const val hms = "5.0.5.300"
    const val timber = "4.7.1"
    const val gson = "2.8.6"
    const val exoPlayer = "2.10.5"
    const val dagger = "2.37"
}

object TestVersions {
    const val junit = "4.13.2"
    const val junitPlatformRunner = "1.7.2"
    const val mockito = "3.11.1"
    const val mockitoKotlin = "3.2.0"
    const val livedataTesting = "1.2.0"
    const val androidxJunit = "1.1.1"
    const val coreTesting = "1.1.1"
}

object BuildDependencies {
    const val androidGradle =
        "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlinGradlePlugin =
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val agConnect =
        "com.huawei.agconnect:agcp:${Versions.agcp}"
    const val safeArgs =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.safeArgs}"
}

object Dependencies {

    object AndroidX {
        const val fragmentKtx =
            "androidx.fragment:fragment-ktx:${Versions.fragmentKtx}"
        const val coreKtx =
            "androidx.core:core-ktx:${Versions.fragmentKtx}"
        const val appCompat =
            "androidx.appcompat:appcompat:${Versions.appCompat}"
        const val constraintlayout =
            "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
        const val lifecycleLivedataKtx =
            "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycleLivedataKtx}"
        const val lifecycleCompiler =
            "androidx.lifecycle:lifecycle-compiler:${Versions.lifecycleLivedataKtx}"
        const val lifeCycleCommon =
            "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycleLivedataKtx}"
        const val archViewModel =
            "androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycleLivedataKtx}"
        const val archComponents =
            "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycleLivedataKtx}"

        object Navigation {
            const val fragmentKtx =
                "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
            const val uiKtx =
                "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"

        }
    }

    object Kotlin {
        const val jdk8 =
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
        const val coroutines =
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}"

        const val android =
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinxCoroutines}"
    }

    object Huawei {
        const val wearengine = "com.huawei.hms:wearengine:${Versions.wearEngine}"
        const val hmsBase = "com.huawei.hms:base:${Versions.hms}"
    }

    object ExoPlayer {
        const val core = "com.google.android.exoplayer:exoplayer-core:${Versions.exoPlayer}"
        const val ui = "com.google.android.exoplayer:exoplayer-ui:${Versions.exoPlayer}"
    }

    object Dagger {
        const val dagger =
            "com.google.dagger:dagger:${Versions.dagger}"
        const val daggerAndroid =
            "com.google.dagger:dagger-android:${Versions.dagger}"
        const val daggerAndroidSupport =
            "com.google.dagger:dagger-android-support:${Versions.dagger}"
        const val daggerCompiler =
            "com.google.dagger:dagger-compiler:${Versions.dagger}"
        const val daggerAndroidProcessor =
            "com.google.dagger:dagger-android-processor:${Versions.dagger}"
    }

    const val gson = "com.google.code.gson:gson:${Versions.gson}"
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
}

object TestDependencies {

    object AndroidX {
        const val junit =
            "androidx.test.ext:junit:${TestVersions.androidxJunit}"
        const val coreTesting =
            "android.arch.core:core-testing:${TestVersions.coreTesting}"
    }

    object JUnit {
        const val junit =
            "junit:junit:${TestVersions.junit}"
        const val junitPlatformRunner =
            "org.junit.platform:junit-platform-runner:${TestVersions.junitPlatformRunner}"
    }

    const val livedataTesting =
        "com.jraska.livedata:testing-ktx:${TestVersions.livedataTesting}"

    object Mockito {
        const val mockitoCore =
            "org.mockito:mockito-core:${TestVersions.mockito}"
        const val mockitoInline =
            "org.mockito:mockito-inline:${TestVersions.mockito}"
        const val mockitoKotlin =
            "com.nhaarman:mockito-kotlin:${TestVersions.mockitoKotlin}"
    }
}