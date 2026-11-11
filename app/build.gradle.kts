plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

// Keep optional legacy artwork separate from the mandatory static exercise resources.
val syncLocalWorkoutArtwork by tasks.registering(Sync::class) {
    from(rootProject.file("design-assets")) {
        include("machines/**", "muscles/**")
    }
    into(layout.buildDirectory.dir("generated/local-workout-artwork"))
}

android {
    namespace = "com.myworkout"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.myworkout"
        minSdk = 36
        targetSdk = 36
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
    // Optional local artwork is packaged for this device, never copied into version control.
    if (!providers.gradleProperty("withoutLocalArt").isPresent) {
        sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/local-workout-artwork").get().asFile)
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
if (!providers.gradleProperty("withoutLocalArt").isPresent) {
    tasks.matching { (it.name.startsWith("merge") && it.name.endsWith("Assets")) || it.name.contains("Lint") || it.name.startsWith("lint") }.configureEach {
        dependsOn(syncLocalWorkoutArtwork)
    }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
