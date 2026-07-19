// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.room) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            // [FIX-TRANSTIVE-BOUNCYCASTLE] Force resolution to version expected by iText 8.0.5
            force("org.bouncycastle:bcprov-jdk18on:1.78.1")
        }
    }
}
