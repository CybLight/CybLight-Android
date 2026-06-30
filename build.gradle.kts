plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.2" apply false

    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

ktlint {
    android.set(true) // поддержка Android‑специфичных правил
    ignoreFailures.set(false)
    verbose.set(true)
}
