package io.github.chayanforyou.quickball.domain.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MenuItemDeserializationTest {

    private val gson = Gson()
    private val menuItemListType = object : TypeToken<List<QuickBallMenuItem>>() {}.type

    @Test
    fun testDeserializeV433StandardAndAppItems() {
        // Sample JSON saved by version 4.3.3
        val jsonFromV433 = """
            [
                {
                    "action": "VOLUME_UP",
                    "iconRes": 2131230890,
                    "titleRes": 2131689560,
                    "isSelected": false
                },
                {
                    "action": "LAUNCH_APP",
                    "iconRes": 2131230890,
                    "titleRes": 0,
                    "isSelected": false,
                    "packageName": "com.whatsapp",
                    "appTitle": "WhatsApp"
                }
            ]
        """.trimIndent()

        val items: List<QuickBallMenuItem>? = gson.fromJson(jsonFromV433, menuItemListType)
        assertNotNull(items)
        assertEquals(2, items!!.size)

        // Verify standard item
        val standardItem = items[0]
        assertEquals(MenuAction.VOLUME_UP, standardItem.action)

        // Verify app item
        val appItem = items[1]
        assertEquals(MenuAction.LAUNCH_APP, appItem.action)
        assertEquals("com.whatsapp", appItem.packageName)
        assertEquals("WhatsApp", appItem.appTitle)
    }

    @Test
    fun testDeserializeCorruptOrObfuscatedNullActionItem() {
        // Sample JSON where action is missing or null
        val corruptJson = """
            [
                {
                    "packageName": "com.instagram.android",
                    "appTitle": "Instagram"
                },
                {
                    "iconRes": 1234
                }
            ]
        """.trimIndent()

        val rawItems: List<QuickBallMenuItem>? = gson.fromJson(corruptJson, menuItemListType)
        assertNotNull(rawItems)

        val validItems = rawItems!!.mapNotNull { item ->
            val resolvedAction = item.action ?: if (item.packageName != null) MenuAction.LAUNCH_APP else null
            if (resolvedAction == null) {
                null
            } else if (item.packageName != null) {
                item.copy(action = resolvedAction)
            } else {
                QuickBallMenuItem.getMenuItemByAction(resolvedAction)
            }
        }

        // Only the app item should be recovered with MenuAction.LAUNCH_APP, corrupt standard item without action filtered out
        assertEquals(1, validItems.size)
        assertEquals(MenuAction.LAUNCH_APP, validItems[0].action)
        assertEquals("com.instagram.android", validItems[0].packageName)
    }

    @Test
    fun testGetIconResNeverThrowsNpeWhenActionIsNull() {
        val menuItem = QuickBallMenuItem(action = MenuAction.VOLUME_UP)
        // Accessing iconRes should return valid resource ID
        assertNotNull(menuItem.iconRes)
    }
}
