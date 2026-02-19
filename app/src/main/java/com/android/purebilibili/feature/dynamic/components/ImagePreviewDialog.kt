// 文件路径: feature/dynamic/components/ImagePreviewDialog.kt
package com.android.purebilibili.feature.dynamic.components

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import android.app.Activity
import android.content.ContextWrapper
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

/**
 *  图片预览对话框 - 支持左右滑动切换和3D立体动画
 */
private val ImagePreviewOpenEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1f)
private val ImagePreviewCloseEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private data class ImagePreviewOverlayRequest(
    val token: Long,
    val images: List<String>,
    val initialIndex: Int,
    val sourceRect: androidx.compose.ui.geometry.Rect?,
    val onDismiss: () -> Unit
)

private object ImagePreviewOverlayController {
    private val _request = MutableStateFlow<ImagePreviewOverlayRequest?>(null)
    val request = _request.asStateFlow()

    fun show(request: ImagePreviewOverlayRequest) {
        _request.value = request
    }

    fun dismiss(token: Long? = null) {
        val current = _request.value ?: return
        if (token == null || current.token == token) {
            _request.value = null
        }
    }
}

@Composable
fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int,
    sourceRect: androidx.compose.ui.geometry.Rect? = null,
    onDismiss: () -> Unit
) {
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val requestToken = remember(images, initialIndex, sourceRect) { System.nanoTime() }

    LaunchedEffect(requestToken) {
        ImagePreviewOverlayController.show(
            ImagePreviewOverlayRequest(
                token = requestToken,
                images = images,
                initialIndex = initialIndex,
                sourceRect = sourceRect,
                onDismiss = { latestOnDismiss() }
            )
        )
    }

    DisposableEffect(requestToken) {
        onDispose {
            ImagePreviewOverlayController.dismiss(requestToken)
        }
    }
}

@Composable
fun ImagePreviewOverlayHost(
    modifier: Modifier = Modifier
) {
    val activeRequest by ImagePreviewOverlayController.request.collectAsState()
    activeRequest?.let { request ->
        ImagePreviewOverlayContent(
            images = request.images,
            initialIndex = request.initialIndex,
            sourceRect = request.sourceRect,
            onDismiss = {
                ImagePreviewOverlayController.dismiss(request.token)
                request.onDismiss()
            },
            modifier = modifier
                .fillMaxSize()
                .zIndex(100f)
        )
    }
}

@Composable
private fun ImagePreviewOverlayContent(
    images: List<String>,
    initialIndex: Int,
    sourceRect: androidx.compose.ui.geometry.Rect? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    
    //  获取 Activity 和 Window 用于沉浸式控制
    val activity = remember {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }
    val window = remember { activity?.window }
    val insetsController = remember {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }
    
    //  保存原始导航栏颜色
    val originalNavBarColor = remember { window?.navigationBarColor ?: android.graphics.Color.BLACK }
    
    //  进入时设置沉浸式导航栏（透明黑色），退出时恢复
    DisposableEffect(Unit) {
        window?.navigationBarColor = Color.Transparent.toArgb()
        insetsController?.isAppearanceLightNavigationBars = false
        
        onDispose {
            window?.navigationBarColor = originalNavBarColor
        }
    }
    
    //  动画状态控制
    // 0f = 关闭/初始状态 (at sourceRect), 1f = 打开状态 (Fullscreen)
    val animateTrigger = remember { androidx.compose.animation.core.Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    
    // 启动入场动画 - 使用轻阻尼弹簧，保留自然惯性
    LaunchedEffect(Unit) {
        animateTrigger.snapTo(0f)
        animateTrigger.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 340, easing = ImagePreviewOpenEasing)
        )
    }
    
    // 触发退场动画
    fun triggerDismiss() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            val dismissMotion = imagePreviewDismissMotion()
            val target = dismissMotion.settleTarget
            animateTrigger.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 280, easing = ImagePreviewCloseEasing)
            )
            onDismiss()
        }
    }

    BackHandler(enabled = !isDismissing) {
        triggerDismiss()
    }

    //  GIF 图片加载器
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }

    //  使用 HorizontalPager 实现滑动切换
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.size }
    )
    
    //  存储权限状态（Android 9 及以下需要）
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    val storagePermission = com.android.purebilibili.core.util.rememberStoragePermissionState { granted ->
        if (granted && pendingSaveUrl != null) {
            // 权限授予后执行保存
            isSaving = true
            scope.launch {
                val success = saveImageToGallery(context, pendingSaveUrl!!)
                isSaving = false
                pendingSaveUrl = null
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (success) "图片已保存到相册" else "保存失败，请重试",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // 当前页的图片 URL
    val currentImageUrl = remember(pagerState.currentPage, images) {
        normalizeImageUrl(images.getOrNull(pagerState.currentPage) ?: "")
    }
    
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
            val constraints = this
            val fullWidth = constraints.maxWidth
            val fullHeight = constraints.maxHeight
            
            val rawProgress = animateTrigger.value
            
            //  计算容器位置和大小
            // 如果切走了或者没有源矩形，则全屏显示（仅淡入淡出）
            val isInitialPage = pagerState.currentPage == initialIndex
            val shouldUseRectAnim = sourceRect != null && isInitialPage
            val transitionFrame = resolveImagePreviewTransitionFrame(
                rawProgress = rawProgress,
                hasSourceRect = shouldUseRectAnim,
                sourceCornerRadiusDp = 12f
            )
            
            val targetLeft = 0.dp
            val targetTop = 0.dp
            val targetWidth = fullWidth
            val targetHeight = fullHeight
            
            val (currentLeft, currentTop, currentWidth, currentHeight) = if (shouldUseRectAnim) {
                val sourceLeft = with(density) { sourceRect!!.left.toDp() }
                val sourceTop = with(density) { sourceRect!!.top.toDp() }
                val sourceWidth = with(density) { sourceRect!!.width.toDp() }
                val sourceHeight = with(density) { sourceRect!!.height.toDp() }
                
                val l = androidx.compose.ui.unit.lerp(sourceLeft, targetLeft, transitionFrame.layoutProgress)
                val t = androidx.compose.ui.unit.lerp(sourceTop, targetTop, transitionFrame.layoutProgress)
                val w = androidx.compose.ui.unit.lerp(sourceWidth, targetWidth, transitionFrame.layoutProgress)
                val h = androidx.compose.ui.unit.lerp(sourceHeight, targetHeight, transitionFrame.layoutProgress)
                
                Quad(l, t, w, h)
            } else {
                Quad(0.dp, 0.dp, fullWidth, fullHeight)
            }
            
            // 1. 背景层 (淡入淡出)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = transitionFrame.visualProgress))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { triggerDismiss() }
                        )
                    }
            )
            
            // 2. 内容层 (缩放位移)
            Box(
                 modifier = Modifier
                     .offset(x = currentLeft, y = currentTop)
                     .size(width = currentWidth, height = currentHeight)
                     .clip(RoundedCornerShape(transitionFrame.cornerRadiusDp.dp))
                     .graphicsLayer {
                         if (!shouldUseRectAnim) {
                             scaleX = transitionFrame.fallbackScale
                             scaleY = transitionFrame.fallbackScale
                         }
                     }
            ) {
                //  使用 HorizontalPager 实现滑动切换 + 3D立体动画
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,  // 预加载相邻页面
                    key = { images.getOrElse(it) { "" } }
                ) { page ->
                    // 计算当前页面的偏移量（0 = 居中，-1 = 左边，1 = 右边）
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    
                    // 🎭 3D 立体旋转动画 - Cube 效果
                    // 仅当完全打开时才应用复杂变换，避免动画冲突
                    val apply3D = transitionFrame.visualProgress > 0.92f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (apply3D) {
                                    //  3D 旋转角度（最大45度）
                                    val rotationAngle = pageOffset * 45f
                                    rotationY = rotationAngle
                                    
                                    //  设置旋转中心点
                                    cameraDistance = 12f * density.density
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                        pivotFractionX = if (pageOffset < 0) 1f else 0f,
                                        pivotFractionY = 0.5f
                                    )
                                    
                                    //  缩放效果
                                    val scale = 1f - (abs(pageOffset) * 0.1f).coerceIn(0f, 0.15f)
                                    scaleX = scale
                                    scaleY = scale
                                    
                                    //  透明度渐变
                                    alpha = 1f - (abs(pageOffset) * 0.3f).coerceIn(0f, 0.5f)
                                }
                            }
                            .pointerInput(Unit) {
                                // 阻止点击穿透到关闭手势
                                detectTapGestures { 
                                     // 点击图片也关闭
                                     triggerDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUrl = remember(images.getOrNull(page)) {
                            normalizeImageUrl(images.getOrNull(page) ?: "")
                        }
                        
                        //  [新增] 使用 ZoomableImage 替换 AsyncImage
                        //  维护当前页面的缩放状态，用于控制 Pager 是否允许滑动
                        var currentScale by remember { mutableFloatStateOf(1f) }
                        
                        ZoomableImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .size(coil.size.Size.ORIGINAL)  //  强制加载原图，避免模糊
                                .addHeader("Referer", "https://www.bilibili.com/")
                                .crossfade(300)
                                .build(),
                            contentDescription = null,
                            imageLoader = gifImageLoader,  //  使用 GIF 加载器
                            modifier = Modifier.fillMaxSize(),
                            onZoomChange = { scale ->
                                currentScale = scale
                                // 当放大时，我们希望禁止 Pager 滑动（但这需要提升状态到 Pager 层级）
                                // 由于这是在 Item 内部，我们不能直接控制 Pager 的 userScrollEnabled
                                // 但 ZoomableImage 内部的手势监听会消费触摸事件，从而自然阻止 Pager 滑动
                                // 只要 pointerInput 的处理得当
                            },
                            onClick = {
                                // 点击图片关闭预览
                                triggerDismiss()
                            }
                        )
                    }
                }
            }
            
            // 3. UI 覆盖层 (淡入淡出) - 包含页码、下载按钮、关闭按钮
            // 只有当动画接近完成时才显示 UI，避免缩放时 UI 挤压
            // 或者始终显示但淡入淡出
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = transitionFrame.visualProgress }
            ) {
                //  页码指示器（圆点样式）
                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()  //  避开导航栏
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        images.forEachIndexed { index, _ ->
                            val isSelected = pagerState.currentPage == index
                            // 动画过渡
                            val dotSize by animateFloatAsState(
                                targetValue = if (isSelected) 10f else 6f,
                                animationSpec = spring(dampingRatio = 0.7f),
                                label = "dotSize"
                            )
                            val dotAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.5f,
                                animationSpec = spring(dampingRatio = 0.7f),
                                label = "dotAlpha"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(dotSize.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = dotAlpha))
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }
                }
                
                // 顶部按钮栏（关闭 + 页码 + 下载）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 关闭按钮
                    FilledIconButton(
                        onClick = { triggerDismiss() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Xmark,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                    
                    //  页码文字
                    if (images.size > 1) {
                        Text(
                            "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    
                    //  下载按钮
                    FilledIconButton(
                        onClick = {
                            if (!isSaving && currentImageUrl.isNotEmpty()) {
                                //  检查权限（Android 10+ 自动授权）
                                if (storagePermission.isGranted) {
                                    isSaving = true
                                    scope.launch {
                                        val success = saveImageToGallery(context, currentImageUrl)
                                        isSaving = false
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                if (success) "图片已保存到相册" else "保存失败，请重试",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    // 保存待执行的 URL，请求权限
                                    pendingSaveUrl = currentImageUrl
                                    storagePermission.request()
                                }
                            }
                        },
                        enabled = !isSaving,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(0.5f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = CupertinoIcons.Default.ArrowDownCircle,
                                contentDescription = "保存图片",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
    }
}

// 辅助数据类
data class Quad(val left: androidx.compose.ui.unit.Dp, val top: androidx.compose.ui.unit.Dp, val width: androidx.compose.ui.unit.Dp, val height: androidx.compose.ui.unit.Dp)

/**
 *  规范化图片 URL
 * 1. 修复协议头（http -> https, // -> https://）
 * 2. 移除分辨率限制参数（@...）以获取原图
 */
private fun normalizeImageUrl(rawSrc: String): String {
    val trimmed = rawSrc.trim()
    var result = when {
        trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("http://") -> trimmed.replace("http://", "https://")
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.isNotEmpty() -> "https://$trimmed"
        else -> ""
    }
    
    //  移除 Bilibili 图片尺寸参数（例如 @640w_400h.webp）以获取最高质量
    if (result.contains("@")) {
        result = result.substringBefore("@")
    }
    
    return result
}

/**
 *  保存图片到相册 - 支持 GIF/WebP 等格式保留
 */
suspend fun saveImageToGallery(context: android.content.Context, imageUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            //  检测图片格式
            val isGif = imageUrl.contains(".gif", ignoreCase = true)
            val isWebp = imageUrl.contains(".webp", ignoreCase = true)
            val isPng = imageUrl.contains(".png", ignoreCase = true)
            
            //  对于 GIF/WebP，直接下载原始字节流保留动画
            if (isGif || isWebp) {
                val url = java.net.URL(imageUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Referer", "https://www.bilibili.com/")
                connection.connect()
                
                if (connection.responseCode != 200) {
                    Log.e("ImagePreview", "Failed to download: ${connection.responseCode}")
                    return@withContext false
                }
                
                val inputStream = connection.inputStream
                val bytes = inputStream.readBytes()
                inputStream.close()
                connection.disconnect()
                
                // 生成文件名
                val extension = when {
                    isGif -> "gif"
                    isWebp -> "webp"
                    else -> "jpg"
                }
                val mimeType = when {
                    isGif -> "image/gif"
                    isWebp -> "image/webp"
                    else -> "image/jpeg"
                }
                val fileName = "BiliPai_${System.currentTimeMillis()}.$extension"
                
                // 使用 MediaStore 保存
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext false
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                
                Log.d("ImagePreview", "Image saved successfully: $fileName")
                return@withContext true
            }
            
            //  对于 JPEG/PNG 等静态图片，使用 Coil 下载并转换
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .addHeader("Referer", "https://www.bilibili.com/")
                .build()
            
            val result = imageLoader.execute(request)
            if (result !is SuccessResult) {
                Log.e("ImagePreview", "Failed to download image: $imageUrl")
                return@withContext false
            }
            
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap == null) {
                Log.e("ImagePreview", "Failed to convert drawable to bitmap")
                return@withContext false
            }
            
            // 生成文件名
            val extension = if (isPng) "png" else "jpg"
            val mimeType = if (isPng) "image/png" else "image/jpeg"
            val fileName = "BiliPai_${System.currentTimeMillis()}.$extension"
            
            // 使用 MediaStore 保存图片
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext false
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val format = if (isPng) android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG
                bitmap.compress(format, 95, outputStream)
            }
            
            // 标记保存完成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            
            Log.d("ImagePreview", "Image saved successfully: $fileName")
            true
        } catch (e: Exception) {
            Log.e("ImagePreview", "Error saving image", e)
            false
        }
    }
}
