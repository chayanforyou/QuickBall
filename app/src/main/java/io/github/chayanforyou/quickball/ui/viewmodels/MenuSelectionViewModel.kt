package io.github.chayanforyou.quickball.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem

class MenuSelectionViewModel : ViewModel() {
    
    private val _selectedMenuItem = MutableLiveData<QuickBallMenuItem?>()
    val selectedMenuItem: LiveData<QuickBallMenuItem?> = _selectedMenuItem
    
    private val _selectedPosition = MutableLiveData<Int>()
    val selectedPosition: LiveData<Int> = _selectedPosition
    
    fun setSelectedPosition(position: Int) {
        _selectedPosition.value = position
    }
    
    fun setSelectedMenuItem(menuItem: QuickBallMenuItem) {
        _selectedMenuItem.value = menuItem
    }
    
    fun clearSelectedMenuItem() {
        _selectedMenuItem.value = null
    }
}
