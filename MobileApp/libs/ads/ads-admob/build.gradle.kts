plugins {
    id("configure-kmp-library-module")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    android {
        namespace = "com.kotlinfoundation.koko.ads.admob"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.libs.ads.adsApi)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
        }

        androidMain.dependencies {
            implementation(libs.google.admob)
        }
    }
}
