import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { stream ->
        keystoreProperties.load(InputStreamReader(stream, Charsets.UTF_8))
    }
}

val devnetRpcUrl = localProperties.getProperty("devnet.rpc.url", "https://api.devnet.solana.com")
val mainnetRpcUrl = localProperties.getProperty(
    "mainnet.rpc.url",
    "https://api.mainnet-beta.solana.com",
)
val alchemyKey = localProperties.getProperty("mainnet.alchemy.key", "")
val heliusKey = localProperties.getProperty("mainnet.helius.key", "")

val alchemyUrl = if (alchemyKey.isNotBlank()) "https://solana-mainnet.g.alchemy.com/v2/$alchemyKey" else ""
val heliusUrl = if (heliusKey.isNotBlank()) "https://mainnet.helius-rpc.com/?api-key=$heliusKey" else ""

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.opencookie.app"
    compileSdk = 36

    base {
        archivesName.set("OpenCookie")
    }

    defaultConfig {
        applicationId = "com.opencookie.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "DEVNET_RPC_URL", "\"$devnetRpcUrl\"")
        buildConfigField("String", "MAINNET_RPC_URL", "\"$mainnetRpcUrl\"")
        buildConfigField("String", "MAINNET_ALCHEMY_URL", "\"$alchemyUrl\"")
        buildConfigField("String", "MAINNET_HELIUS_URL", "\"$heliusUrl\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val keyAliasProp = keystoreProperties.getProperty("keyAlias")
                val keyPasswordProp = keystoreProperties.getProperty("keyPassword")
                val storeFileProp = keystoreProperties.getProperty("storeFile")
                val storePasswordProp = keystoreProperties.getProperty("storePassword")

                if (!storeFileProp.isNullOrBlank()) {
                    val candidateFromRoot = rootProject.file(storeFileProp)
                    val candidateFromApp = file(storeFileProp)
                    val absoluteCandidate = File(storeFileProp)

                    val targetFile = when {
                        absoluteCandidate.isAbsolute && absoluteCandidate.exists() -> absoluteCandidate
                        candidateFromRoot.exists() -> candidateFromRoot
                        candidateFromApp.exists() -> candidateFromApp
                        else -> candidateFromRoot
                    }

                    storeFile = targetFile
                    storePassword = storePasswordProp
                    keyAlias = keyAliasProp
                    keyPassword = keyPasswordProp
                }
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEFAULT_CLUSTER", "\"mainnetbeta\"")
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("String", "DEFAULT_CLUSTER", "\"mainnetbeta\"")
            buildConfigField("String", "DEVNET_RPC_URL", "\"\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("com.google.dagger:hilt-android:2.53.1")
    ksp("com.google.dagger:hilt-compiler:2.53.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    val ktorVersion = "3.0.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.1.0")
    implementation("com.solanamobile:web3-solana:0.3.1")

    implementation("androidx.security:security-crypto:1.1.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
