buildscript {
    repositories {
        google()
        jcenter()
        maven("https://developer.huawei.com/repo/")
    }
    dependencies {
        classpath(BuildDependencies.androidGradle)
        classpath(BuildDependencies.safeArgs)
        classpath(BuildDependencies.kotlinGradlePlugin)
        classpath(BuildDependencies.agConnect)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://developer.huawei.com/repo/")
    }
}

task("clean") {
    delete(rootProject.buildDir)
}
