plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id ("androidx.navigation.safeargs.kotlin")


}

android {
    namespace = "pe.edu.trujidelivery"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.edu.trujidelivery"
        minSdk = 25
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.core.ktx)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // OpenStreetMap y Nominatim
    implementation(libs.osmdroid)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Permisos
    implementation(libs.permissions.dispatcher)

    // Google Play Services para ubicación
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Glide para manejo de imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation ("com.google.firebase:firebase-bom:32.1.1")
    implementation ("com.google.firebase:firebase-firestore-ktx")

    //Seleccionar imagens desde la Galeria
    implementation("androidx.activity:activity:1.8.0")

    implementation("com.google.firebase:firebase-storage")


    //
    implementation ("com.google.firebase:firebase-auth-ktx:21.1.0")

//Navegation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")

    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.google.firebase:firebase-messaging:24.0.0")
    implementation ("com.google.firebase:firebase-functions:20.4.0")

    //IMGBB
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
}