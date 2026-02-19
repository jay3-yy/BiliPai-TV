package com.android.purebilibili.feature.login

// 注入到 WebView 的 JS/CSS，用于优化登录页面布局
// 使用延迟执行确保页面完全加载后再修改样式
const val WEB_LOGIN_INJECT_JS = """
    javascript:(function() {
        setTimeout(function() {
            var style = document.createElement('style');
            style.innerHTML = `
                /* 只调整协议文字的位置，避免和按钮重叠 */
                .clause-tip, .clause-checkbox, .clause-wrapper,
                .protocol, .agreement-box, .user-agreement {
                    margin-top: 30px !important;
                    padding-top: 15px !important;
                }
                
                /* 确保登录按钮有足够的下边距 */
                .login-btn-wrap, .btn-login, .submit-btn {
                    margin-bottom: 25px !important;
                }
            `;
            document.head.appendChild(style);
        }, 500);
    })()
"""
