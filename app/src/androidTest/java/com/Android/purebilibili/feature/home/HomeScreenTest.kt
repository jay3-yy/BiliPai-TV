package com.android.purebilibili.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HomeScreen Compose UI 测试
 * 
 * 测试覆盖:
 * - 分类标签显示
 * - 视频卡片渲染
 * - 交互响应
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun categoryTabs_shouldDisplayAllCategories() {
        // Given: 显示 CategoryTabRow
        composeTestRule.setContent {
            com.android.purebilibili.feature.home.components.CategoryTabRow(
                categories = listOf("推荐", "热门", "直播", "追番", "影视"),
                selectedIndex = 0,
                onCategorySelected = {}
            )
        }
        
        // Then: 所有分类应该可见
        composeTestRule.onNodeWithText("推荐").assertIsDisplayed()
        composeTestRule.onNodeWithText("热门").assertIsDisplayed()
        composeTestRule.onNodeWithText("直播").assertIsDisplayed()
    }
    
    @Test
    fun categoryTab_clickShouldTriggerCallback() {
        // Given: 记录点击的索引
        var clickedIndex = -1
        
        composeTestRule.setContent {
            com.android.purebilibili.feature.home.components.CategoryTabRow(
                categories = listOf("推荐", "热门", "直播"),
                selectedIndex = 0,
                onCategorySelected = { clickedIndex = it }
            )
        }
        
        // When: 点击 "热门"
        composeTestRule.onNodeWithText("热门").performClick()
        
        // Then: 应回调索引 1
        assert(clickedIndex == 1) { "Expected index 1, but got $clickedIndex" }
    }
    
    @Test
    fun bottomNavBar_shouldDisplayAllItems() {
        // Given: 显示底部导航栏
        composeTestRule.setContent {
            com.android.purebilibili.feature.home.components.FrostedBottomBar(
                currentItem = com.android.purebilibili.feature.home.components.BottomNavItem.HOME,
                onItemClick = {}
            )
        }
        
        // Then: 所有导航项应该可见
        composeTestRule.onNodeWithText("首页").assertIsDisplayed()
        composeTestRule.onNodeWithText("动态").assertIsDisplayed()
        composeTestRule.onNodeWithText("历史").assertIsDisplayed()
        composeTestRule.onNodeWithText("我的").assertIsDisplayed()
    }
}
