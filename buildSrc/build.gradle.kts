repositories {
    jcenter()
    google()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.android.tools.build:gradle:4.2.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")

    implementation(gradleApi())
}
