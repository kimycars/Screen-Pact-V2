import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.scrollban.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.scrollban.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    /*
     * Firma de release: la clave vive FUERA del repo (Documents\claves). Sin ese fichero
     * (por ejemplo en GitHub Actions) el release sale sin firmar y solo vale el debug.
     * Perder esta clave = no poder actualizar la app nunca más: guarda copia de la carpeta claves.
     */
    val keystoreProps = file(System.getenv("SP_KEYSTORE_PROPS") ?: "C:/Users/Usuario/Documents/claves/screenpact-keystore.properties")
    signingConfigs {
        if (keystoreProps.exists()) {
            val p = Properties().apply { keystoreProps.inputStream().use { load(it) } }
            create("release") {
                storeFile = file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    // Esquema de la base de datos por versión, para comprobar las migraciones (app/schemas).
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore for simple key-value (own secret)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // QR generation + scanning (ZXing)
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager (periodic checks fallback)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Pago de Pro en una pestaña dentro de la app (Custom Tab), sin salir a otro navegador.
    implementation("androidx.browser:browser:1.8.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/*
 * Guardián del sistema de diseño: fuera de ui/theme no puede haber medidas (.dp/.sp) ni colores
 * (Color(0x…)) escritos a mano. Todo sale de los tokens de ui/theme (Theme.kt y Tokens.kt) y de
 * las piezas de ui/components. Corre antes de cada build; si encuentra algo, el build falla
 * y dice el fichero y la línea.
 */
val checkDesignTokens by tasks.registering {
    group = "verification"
    description = "Falla si hay valores de diseño escritos a mano fuera de ui/theme."
    val srcDir = file("src/main/kotlin")
    inputs.dir(srcDir)
    doLast {
        val pattern = Regex("""\b\d+(\.\d+)?\.(dp|sp)\b|Color\(0x""")
        val offenders = srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.invariantSeparatorsPath.contains("/ui/theme/") }
            .flatMap { f ->
                f.readLines().mapIndexedNotNull { i, line ->
                    if (pattern.containsMatchIn(line)) "${f.relativeTo(srcDir).invariantSeparatorsPath}:${i + 1}: ${line.trim()}" else null
                }
            }
            .toList()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Valores de diseño escritos a mano. Usa los tokens de ui/theme (Sb, Space, Sizes, Strokes, Shapes, TextStyles):\n" +
                    offenders.joinToString("\n")
            )
        }
    }
}
tasks.named("preBuild") { dependsOn(checkDesignTokens) }
