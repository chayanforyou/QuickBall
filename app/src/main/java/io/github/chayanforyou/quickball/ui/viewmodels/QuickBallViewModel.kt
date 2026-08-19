package io.github.chayanforyou.quickball.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.chayanforyou.quickball.domain.AppDefaults
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickBallUiState(
    val isQuickBallEnabled: Boolean = true,
    val showOnLockScreen: Boolean = false,
    val hideOnLandscape: Boolean = false,
    val stickToEdge: Boolean = true,
    val ballSize: Float = AppDefaults.BALL_SIZE,
    val ballColor: Int = AppDefaults.BALL_COLOR,
    val ballIconColor: Int = AppDefaults.BALL_ICON_COLOR,
    val menuColor: Int = AppDefaults.MENU_COLOR,
    val menuIconColor: Int = AppDefaults.MENU_ICON_COLOR,
    val menuSize: Float = AppDefaults.MENU_SIZE,
    val menuIconSize: Float = AppDefaults.MENU_ICON_SIZE,
    val menuRadius: Float = AppDefaults.MENU_RADIUS,
    val toastBgColor: Int = AppDefaults.TOAST_BG_COLOR,
    val toastFgColor: Int = AppDefaults.TOAST_FG_COLOR,
    val pillColor: Int = AppDefaults.PILL_COLOR,
    val pillHeight: Float = AppDefaults.PILL_HEIGHT,
    val pillThickness: Float = AppDefaults.PILL_THICKNESS,
    val pillTouchWidth: Float = AppDefaults.PILL_TOUCH_WIDTH,
    val pillArcAngle: Float = AppDefaults.PILL_ARC_ANGLE,
    val isPillGestureEnabled: Boolean = AppDefaults.PILL_GESTURE_ENABLED,
    val pillDoubleTapAction: String = AppDefaults.PILL_DOUBLE_TAP_ACTION,
    val pillTripleTapAction: String = AppDefaults.PILL_TRIPLE_TAP_ACTION,
    val pillLongPressAction: String = AppDefaults.PILL_LONG_PRESS_ACTION,
    val pillSwipeUpAction: String = AppDefaults.PILL_SWIPE_UP_ACTION,
    val pillSwipeDownAction: String = AppDefaults.PILL_SWIPE_DOWN_ACTION,
    val isHapticFeedbackEnabled: Boolean = AppDefaults.HAPTIC_FEEDBACK_ENABLED,
    val hapticIntensity: String = AppDefaults.HAPTIC_INTENSITY,
    val isAccessibilityGranted: Boolean = false,
    val selectedMenuItems: List<QuickBallMenuItem> = emptyList(),
    val autoHideApps: Set<String> = emptySet()
)

class QuickBallViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreference.getInstance(application)

    private val _uiState = MutableStateFlow(
        QuickBallUiState(
            isQuickBallEnabled = prefs.isQuickBallEnabled,
            showOnLockScreen = prefs.isShowOnLockScreenEnabled,
            hideOnLandscape = prefs.isHideOnLandscapeEnabled,
            stickToEdge = prefs.isStickToEdgeEnabled,
            ballSize = prefs.ballSize,
            ballColor = prefs.ballColor,
            ballIconColor = prefs.ballIconColor,
            menuColor = prefs.menuColor,
            menuIconColor = prefs.menuIconColor,
            menuSize = prefs.menuSize,
            menuIconSize = prefs.menuIconSize,
            menuRadius = prefs.menuRadius,
            toastBgColor = prefs.toastBgColor,
            toastFgColor = prefs.toastFgColor,
            pillColor = prefs.pillColor,
            pillHeight = prefs.pillHeight,
            pillThickness = prefs.pillThickness,
            pillTouchWidth = prefs.pillTouchWidth,
            pillArcAngle = prefs.pillArcAngle,
            isPillGestureEnabled = prefs.isPillGestureEnabled,
            pillDoubleTapAction = prefs.pillDoubleTapAction,
            pillTripleTapAction = prefs.pillTripleTapAction,
            pillLongPressAction = prefs.pillLongPressAction,
            pillSwipeUpAction = prefs.pillSwipeUpAction,
            pillSwipeDownAction = prefs.pillSwipeDownAction,
            isHapticFeedbackEnabled = prefs.isHapticFeedbackEnabled,
            hapticIntensity = prefs.hapticIntensity,
            selectedMenuItems = prefs.selectedMenuItems,
            autoHideApps = prefs.autoHideApps
        )
    )
    val uiState: StateFlow<QuickBallUiState> = _uiState.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.isOnboardingCompleted)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _targetIndex = MutableStateFlow(0)
    val targetIndex: StateFlow<Int> = _targetIndex.asStateFlow()

    fun setTargetIndex(index: Int) {
        _targetIndex.value = index
    }

    fun refreshPermissionsState(isAccessibilityGranted: Boolean) {
        _uiState.update {
            it.copy(
                isAccessibilityGranted = isAccessibilityGranted,
                isQuickBallEnabled = prefs.isQuickBallEnabled
            )
        }
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.isOnboardingCompleted = completed
        _isOnboardingCompleted.value = completed
    }

    fun setQuickBallEnabled(enabled: Boolean) {
        prefs.isQuickBallEnabled = enabled
        _uiState.update { it.copy(isQuickBallEnabled = enabled) }
    }

    fun setShowOnLockScreen(enabled: Boolean) {
        prefs.isShowOnLockScreenEnabled = enabled
        _uiState.update { it.copy(showOnLockScreen = enabled) }
    }

    fun setHideOnLandscape(enabled: Boolean) {
        prefs.isHideOnLandscapeEnabled = enabled
        _uiState.update { it.copy(hideOnLandscape = enabled) }
    }

    fun setStickToEdge(enabled: Boolean) {
        prefs.isStickToEdgeEnabled = enabled
        _uiState.update { it.copy(stickToEdge = enabled) }
    }

    fun setBallSize(size: Float) {
        prefs.ballSize = size
        _uiState.update { it.copy(ballSize = size) }
    }

    fun setBallColor(color: Int) {
        prefs.ballColor = color
        _uiState.update { it.copy(ballColor = color) }
    }

    fun setBallIconColor(color: Int) {
        prefs.ballIconColor = color
        _uiState.update { it.copy(ballIconColor = color) }
    }

    fun setMenuColor(color: Int) {
        prefs.menuColor = color
        _uiState.update { it.copy(menuColor = color) }
    }

    fun setMenuIconColor(color: Int) {
        prefs.menuIconColor = color
        _uiState.update { it.copy(menuIconColor = color) }
    }

    fun setMenuSize(size: Float) {
        prefs.menuSize = size
        _uiState.update { it.copy(menuSize = size) }
    }

    fun setMenuIconSize(size: Float) {
        prefs.menuIconSize = size
        _uiState.update { it.copy(menuIconSize = size) }
    }

    fun setMenuRadius(radius: Float) {
        prefs.menuRadius = radius
        _uiState.update { it.copy(menuRadius = radius) }
    }

    fun setToastBgColor(color: Int) {
        prefs.toastBgColor = color
        _uiState.update { it.copy(toastBgColor = color) }
    }

    fun setToastFgColor(color: Int) {
        prefs.toastFgColor = color
        _uiState.update { it.copy(toastFgColor = color) }
    }

    fun setPillColor(color: Int) {
        prefs.pillColor = color
        _uiState.update { it.copy(pillColor = color) }
    }

    fun setPillHeight(height: Float) {
        prefs.pillHeight = height
        _uiState.update { it.copy(pillHeight = height) }
    }

    fun setPillThickness(thickness: Float) {
        prefs.pillThickness = thickness
        _uiState.update { it.copy(pillThickness = thickness) }
    }

    fun setPillTouchWidth(width: Float) {
        prefs.pillTouchWidth = width
        _uiState.update { it.copy(pillTouchWidth = width) }
    }

    fun setPillArcAngle(angle: Float) {
        prefs.pillArcAngle = angle
        _uiState.update { it.copy(pillArcAngle = angle) }
    }

    fun setPillGestureEnabled(enabled: Boolean) {
        prefs.isPillGestureEnabled = enabled
        _uiState.update { it.copy(isPillGestureEnabled = enabled) }
    }

    fun setPillDoubleTapAction(action: String) {
        prefs.pillDoubleTapAction = action
        _uiState.update { it.copy(pillDoubleTapAction = action) }
    }

    fun setPillTripleTapAction(action: String) {
        prefs.pillTripleTapAction = action
        _uiState.update { it.copy(pillTripleTapAction = action) }
    }

    fun setPillLongPressAction(action: String) {
        prefs.pillLongPressAction = action
        _uiState.update { it.copy(pillLongPressAction = action) }
    }

    fun setPillSwipeUpAction(action: String) {
        prefs.pillSwipeUpAction = action
        _uiState.update { it.copy(pillSwipeUpAction = action) }
    }

    fun setPillSwipeDownAction(action: String) {
        prefs.pillSwipeDownAction = action
        _uiState.update { it.copy(pillSwipeDownAction = action) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.isHapticFeedbackEnabled = enabled
        _uiState.update { it.copy(isHapticFeedbackEnabled = enabled) }
    }

    fun setHapticIntensity(intensity: String) {
        prefs.hapticIntensity = intensity
        _uiState.update { it.copy(hapticIntensity = intensity) }
    }

    fun resetBallSize() {
        setBallSize(AppDefaults.BALL_SIZE)
    }

    fun resetBallColor() {
        setBallColor(AppDefaults.BALL_COLOR)
    }

    fun resetBallIconColor() {
        setBallIconColor(AppDefaults.BALL_ICON_COLOR)
    }

    fun resetMenuColor() {
        setMenuColor(AppDefaults.MENU_COLOR)
    }

    fun resetMenuIconColor() {
        setMenuIconColor(AppDefaults.MENU_ICON_COLOR)
    }

    fun resetMenuSize() {
        setMenuSize(AppDefaults.MENU_SIZE)
    }

    fun resetMenuIconSize() {
        setMenuIconSize(AppDefaults.MENU_ICON_SIZE)
    }

    fun resetMenuRadius() {
        setMenuRadius(AppDefaults.MENU_RADIUS)
    }

    fun resetToastBgColor() {
        setToastBgColor(AppDefaults.TOAST_BG_COLOR)
    }

    fun resetToastFgColor() {
        setToastFgColor(AppDefaults.TOAST_FG_COLOR)
    }

    fun resetPillColor() {
        setPillColor(AppDefaults.PILL_COLOR)
    }

    fun resetPillHeight() {
        setPillHeight(AppDefaults.PILL_HEIGHT)
    }

    fun resetPillThickness() {
        setPillThickness(AppDefaults.PILL_THICKNESS)
    }

    fun resetPillTouchWidth() {
        setPillTouchWidth(AppDefaults.PILL_TOUCH_WIDTH)
    }

    fun resetPillArcAngle() {
        setPillArcAngle(AppDefaults.PILL_ARC_ANGLE)
    }

    fun loadSelectedMenuItems() {
        viewModelScope.launch {
            val items = prefs.selectedMenuItems
            _uiState.update { it.copy(selectedMenuItems = items) }
        }
    }

    fun selectMenuItem(index: Int, menuItem: QuickBallMenuItem) {
        val currentItems = _uiState.value.selectedMenuItems.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = menuItem
            prefs.selectedMenuItems = currentItems
            _uiState.update { it.copy(selectedMenuItems = currentItems) }
        }
    }

    fun updateMenuItems(newItems: List<QuickBallMenuItem>) {
        prefs.selectedMenuItems = newItems
        _uiState.update { it.copy(selectedMenuItems = newItems) }
    }

    fun setAutoHideApps(apps: Set<String>) {
        prefs.autoHideApps = apps
        _uiState.update { it.copy(autoHideApps = apps) }
    }

    fun toggleAutoHideApp(packageName: String) {
        val currentSet = _uiState.value.autoHideApps.toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
        } else {
            currentSet.add(packageName)
        }
        setAutoHideApps(currentSet)
    }
}
