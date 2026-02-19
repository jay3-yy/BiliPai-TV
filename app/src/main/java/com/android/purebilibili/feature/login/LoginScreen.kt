package com.android.purebilibili.feature.login

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel

// Enums
enum class LoginMethod {
    QR_CODE,
    PHONE_SMS
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedMethod by remember { mutableStateOf(LoginMethod.QR_CODE) }

    // Handle navigation when login is successful
    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            onLoginSuccess()
        }
    }
    
    // System Bar Handling
    val activity = LocalActivity.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    DisposableEffect(isDark, activity) {
        val window = activity?.window
        val insetsController = if (window != null) WindowInsetsControllerCompat(window, view) else null
        
        val originalStatusBarColor = window?.statusBarColor ?: 0
        val originalNavBarColor = window?.navigationBarColor ?: 0
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        val originalLightNavBars = insetsController?.isAppearanceLightNavigationBars ?: true
        
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            insetsController?.isAppearanceLightStatusBars = !isDark
            insetsController?.isAppearanceLightNavigationBars = !isDark
        }
        
        onDispose {
            if (window != null) {
                window.statusBarColor = originalStatusBarColor
                window.navigationBarColor = originalNavBarColor
                insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
                insetsController?.isAppearanceLightNavigationBars = originalLightNavBars
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadQrCode()
    }
    
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginBackground()

        IOSLoginLayout(
            selectedMethod = selectedMethod,
            onMethodChange = { selectedMethod = it },
            state = state,
            viewModel = viewModel,
            onClose = onClose
        )
    }
}

@Composable
private fun IOSLoginLayout(
    selectedMethod: LoginMethod,
    onMethodChange: (LoginMethod) -> Unit,
    state: LoginState,
    viewModel: LoginViewModel,
    onClose: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val isWide = maxWidth >= 840.dp
        val sheetModifier = if (isWide) {
            Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 980.dp)
                .heightIn(min = 560.dp, max = 700.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            TopBar(
                onClose = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            )

            GlassCard(
                modifier = sheetModifier.align(Alignment.Center)
            ) {
                if (isWide) {
                    WideLoginSheetContent(
                        selectedMethod = selectedMethod,
                        onMethodChange = onMethodChange,
                        state = state,
                        viewModel = viewModel
                    )
                } else {
                    CompactLoginSheetContent(
                        selectedMethod = selectedMethod,
                        onMethodChange = onMethodChange,
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactLoginSheetContent(
    selectedMethod: LoginMethod,
    onMethodChange: (LoginMethod) -> Unit,
    state: LoginState,
    viewModel: LoginViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandingHeader(isSmall = false)
        Spacer(modifier = Modifier.height(24.dp))
        LoginMethodTabs(selectedMethod, onMethodChange)
        Spacer(modifier = Modifier.height(12.dp))
        LoginContentArea(
            selectedMethod = selectedMethod,
            state = state,
            viewModel = viewModel,
            onRefreshQr = { viewModel.loadQrCode() },
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "登录即代表同意用户协议和隐私政策",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun WideLoginSheetContent(
    selectedMethod: LoginMethod,
    onMethodChange: (LoginMethod) -> Unit,
    state: LoginState,
    viewModel: LoginViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            BrandingHeader(isSmall = false)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "扫码登录推荐用于高画质播放",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "手机号登录适合快速验证，扫码登录更稳定。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
        ) {
            LoginMethodTabs(selectedMethod, onMethodChange)
            Spacer(modifier = Modifier.height(12.dp))
            LoginContentArea(
                selectedMethod = selectedMethod,
                state = state,
                viewModel = viewModel,
                onRefreshQr = { viewModel.loadQrCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "继续即表示你同意用户协议与隐私政策。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun LoginContentArea(
    selectedMethod: LoginMethod,
    state: LoginState,
    viewModel: LoginViewModel,
    onRefreshQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedMethod,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) + slideInVertically { height -> height / 20 } togetherWith
            fadeOut(animationSpec = tween(300)) + slideOutVertically { height -> -height / 20 }
        },
        label = "login_content",
        modifier = modifier
    ) { method ->
        when (method) {
            LoginMethod.QR_CODE -> QrCodeLoginContent(state, onRefreshQr)
            LoginMethod.PHONE_SMS -> PhoneLoginContent(state, viewModel)
        }
    }
}
