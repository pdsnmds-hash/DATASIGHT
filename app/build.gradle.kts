import java.util.Properties

plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("org.jetbrains.kotlin.plugin.serialization")
 id("com.google.devtools.ksp")
}
val uploadKeyPropertiesFile = rootProject.file("release/upload-key.properties")
val uploadKeyProperties = Properties().apply {
 if (uploadKeyPropertiesFile.isFile) uploadKeyPropertiesFile.inputStream().use { load(it) }
}
fun uploadKeyValue(name: String): String = requireNotNull(uploadKeyProperties.getProperty(name)?.takeIf { it.isNotEmpty() }) {
 "Missing release signing property: $name"
}

android {
 namespace = "com.datasight.prompts"
 compileSdk = 36
 defaultConfig { applicationId = "com.datasight.prompts"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0"; ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 signingConfigs {
  create("release") {
   if (uploadKeyPropertiesFile.isFile) {
    storeFile = rootProject.file(uploadKeyValue("storeFile"))
    storePassword = uploadKeyValue("storePassword")
    keyAlias = uploadKeyValue("keyAlias")
    keyPassword = uploadKeyValue("keyPassword")
   }
  }
 }
 buildTypes {
  getByName("release") {
   signingConfig = signingConfigs.getByName("release")
   isMinifyEnabled = false
  }
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.12.01"))
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.activity:activity-compose:1.12.4")
 // Referenced by our manifest override; keep the provider on the compile classpath.
 implementation("androidx.startup:startup-runtime:1.2.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.navigation:navigation-compose:2.9.8")
 implementation("androidx.room:room-runtime:2.8.5")
 implementation("androidx.room:room-ktx:2.8.5")
 ksp("androidx.room:room-compiler:2.8.5")
 implementation("androidx.datastore:datastore-preferences:1.3.0")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
 testImplementation("junit:junit:4.13.2")
}

val checkUploadSigning by tasks.registering {
 doLast {
  check(uploadKeyPropertiesFile.isFile) {
   "Release signing is required. Prepare release/upload-key.properties using Codemagic identities."
  }
  check(rootProject.file(uploadKeyValue("storeFile")).isFile) { "Release keystore file is unavailable." }
 }
}
tasks.configureEach {
 if (name in setOf("packageReleaseBundle", "packageRelease", "bundleRelease", "assembleRelease")) {
  dependsOn(checkUploadSigning)
 }
}
