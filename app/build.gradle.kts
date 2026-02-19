plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Compose 编译器插件
    id("org.jetbrains.kotlin.plugin.compose")
    // JSON 序列化插件
    id("org.jetbrains.kotlin.plugin.serialization")
    // Room 数据库编译插件
    id("com.google.devtools.ksp")
    // 🔥 Firebase 相关插件
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.android.purebilibili"
    compileSdk = 36

    // 投屏服务进程开关：
    // 默认独立 :cast 进程（通过 CastBridgeService 做跨进程 IPC）
    // 如需快速回滚，可传 -PcastServiceProcess=com.android.purebilibili
    val castServiceProcess =
        (project.findProperty("castServiceProcess") as String?) ?: ":cast"

    defaultConfig {
        applicationId = "com.android.purebilibili"
        minSdk = 26
        targetSdk = 35  // 保持35以避免Android 16的新运行时行为
        // 🔥🔥 [版本号] 发布新版前记得更新！格式：versionCode +1, versionName 递增
        // 更新日志：CHANGELOG.md
        versionCode = 74
        versionName = "6.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 👇👇👇 指定打包的 CPU 架构（64 位 + 32 位）👇👇👇
        ndk {
            // arm64-v8a: 现代 64 位真机
            // armeabi-v7a: 兼容 32 位设备/盒子
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        manifestPlaceholders["castServiceProcess"] = castServiceProcess
    }
    
    // 🔥 ABI 分包 - 暂时禁用，生成包含多 ABI 的通用 APK
    splits {
        abi {
            isEnable = false
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            // Disable PNG crunching to avoid AAPT errors
            isCrunchPngs = false
            // 🔥 启用 R8 代码压缩
            isMinifyEnabled = true
            // 🔥 启用资源压缩 (移除未使用的资源)
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Explicitly disable mapping file upload to avoid network errors
            // Explicitly disable mapping file upload to avoid network errors
            pluginManager.withPlugin("com.google.firebase.crashlytics") {
                configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                    mappingFileUploadEnabled = false
                }
            }
        }
        debug {
            // Debug 构建保持快速编译
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 🔥 排除不必要的文件以减小体积
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/kotlin/**"
            excludes += "DebugProbesKt.bin"
            // 📺 Cling DLNA 库冲突文件
            excludes += "META-INF/beans.xml"
        }
    }
    
    // 🚀 启用 JUnit 5
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        // 🔥 允许 Android 类在单元测试中返回默认值而非抛出异常
        unitTests.isReturnDefaultValues = true
    }
    
    // 🔥 自定义 APK 输出文件名
    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "BiliPai-${variant.versionName}-universal.apk"
        }
    }
}

// 🔥 Compose 编译器性能指标 (仅在需要分析时启用，会拖慢编译速度)
// composeCompiler {
//     reportsDestination = layout.buildDirectory.dir("compose_reports")
//     metricsDestination = layout.buildDirectory.dir("compose_metrics")
// }

dependencies {
    // --- 1. Compose UI ---
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))  // 🔥 更新到最新版本
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")  // 🚀 For AppCompatDelegate night mode
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1") // [新增] 窗口大小类
    // 图标扩展库 (全屏、设置图标等)
    implementation("androidx.compose.material:material-icons-extended")

    // --- 2. Network (网络请求) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    // 🔥 Brotli Decompression (for Bilibili Live Danmaku ProtoVer=3)
    implementation("org.brotli:dec:0.1.2")

    // --- 3. Image (图片加载) ---
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")  // 🔥 GIF 动图支持
    
    // --- 3.1 Palette (颜色提取 - 动态取色) ---
    implementation("androidx.palette:palette-ktx:1.0.0")
    
    // --- 3.2 Lottie (动画效果) ---
    implementation("com.airbnb.android:lottie-compose:6.6.2")
    
    // --- 3.3 Haze (毛玻璃效果) ---
    implementation("dev.chrisbanes.haze:haze:1.7.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.1")
    
    // --- 3.4 Shimmer (骨架屏加载) ---
    implementation("com.valentinilk.shimmer:compose-shimmer:1.2.0")
    
    // --- 3.5 Compose Cupertino (iOS 风格 UI 组件) ---
    // 提供 iOS 风格的 Switch、Button、Picker、Dialog 等组件
    implementation("io.github.alexzhirkevich:cupertino:0.1.0-alpha04")
    implementation("io.github.alexzhirkevich:cupertino-adaptive:0.1.0-alpha04")
    // 🍎 800+ iOS SF Symbols 风格图标
    implementation("io.github.alexzhirkevich:cupertino-icons-extended:0.1.0-alpha04")
    
    // --- 3.6 Orbital (iOS 风格共享元素动画) ---
    // 提供流畅的共享元素过渡、尺寸变换、位置移动动画
    implementation("com.github.skydoves:orbital:0.4.0")
    
    // --- 3.7 Startup (应用初始化) ---
    implementation("androidx.startup:startup-runtime:1.1.1")
    
    // --- 3.8 Backdrop (液态玻璃效果) ---
    // 提供透镜折射、玻璃高光、连续圆角等 iOS/visionOS 风格视觉效果
    implementation("io.github.kyant0:backdrop:1.0.0")


    // --- 4. Player (视频播放器 Media3) ---
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.0")  // 🔥 HLS 直播流支持
    implementation("androidx.media3:media3-ui:1.3.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media:media:1.7.0")

    // --- 5. Danmaku (弹幕引擎) ---
    // 🔥 使用 ByteDance DanmakuRenderEngine - 轻量级高性能弹幕渲染引擎
    implementation("com.github.bytedance:DanmakuRenderEngine:v0.1.0")
    
    // 注：FFmpegKit 已于 2025 年停止维护，改用 ExoPlayer 直接播放分离音视频

    // --- 6. Database (Room 数据库) ---
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // --- 7. DataStore (本地存储 Cookie/设置) ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- 8. Utils (工具类) ---
    // 二维码生成
    implementation("com.google.zxing:core:3.5.3")
    // Pinyin 拼音转换 (用于模糊搜索)
    implementation("com.belerweb:pinyin4j:2.5.0")
    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-process:2.9.4")  // 🔋 ProcessLifecycleOwner 后台检测
    
    // --- 8.1 WorkManager (后台下载任务) ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // [新增] ML Kit 人脸检测（远程模型，减少 APK 体积）
    // 使用 Play Services 动态下发模型，不把人脸模型打进 APK。
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    
    // --- 8.2 DLNA & Local Proxy (投屏) ---
    // DLNA Casting (Cling)
    implementation("org.fourthline.cling:cling-core:2.1.2")
    implementation("org.fourthline.cling:cling-support:2.1.2")
    // Jetty (Cling 传输层依赖)
    implementation("org.eclipse.jetty:jetty-server:8.1.22.v20160922")
    implementation("org.eclipse.jetty:jetty-servlet:8.1.22.v20160922")
    implementation("org.eclipse.jetty:jetty-client:8.1.22.v20160922")
    implementation("javax.servlet:javax.servlet-api:3.1.0")
    
    // NanoHTTPD (Lightweight local proxy server)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    implementation("androidx.navigation:navigation-compose:2.9.0")
    
    // --- 9. SplashScreen (启动屏支持) ---
    implementation("androidx.core:core-splashscreen:1.2.0-alpha02")
    
    // --- 10. ProfileInstaller (启动优化) ---
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    
    // --- 11. Firebase (崩溃追踪和分析) ---
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // --- 11. Debug (调试工具) ---
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // 🔥 LeakCanary - 内存泄漏检测 (仅 Debug 构建)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
    
    // --- 12. Testing (测试框架) ---
    // JUnit 4 (兼容旧测试)
    testImplementation("junit:junit:4.13.2")
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    // JUnit 4 兼容层 (允许 JUnit 5 运行 JUnit 4 测试)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    // Kotlin Test (提供 assertEquals, assertTrue 等断言)
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    // MockK for Kotlin mocking
    testImplementation("io.mockk:mockk:1.13.9")
    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    // Turbine for Flow testing
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // --- 13. Android Instrumented Tests ---
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
