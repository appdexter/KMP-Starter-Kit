plugins {
    id("configure-kmp-library-module")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    android {
        namespace = "com.kotlinfoundation.koko.ads.none"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.libs.ads.adsApi)
            implementation(libs.compose.runtime)
            implementation(libs.koin.core)
        }
    }
}
