plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 24
        targetSdk = 31
    }

    namespace = "com.otaliastudios.cameraview"
}

dependencies {
    api("androidx.exifinterface:exifinterface:1.3.3")
    api("androidx.lifecycle:lifecycle-common:2.3.1")
    api("com.google.android.gms:play-services-tasks:17.2.1")
    implementation("androidx.annotation:annotation:1.2.0")
    implementation("com.otaliastudios.opengl:egloo:0.6.1")

}