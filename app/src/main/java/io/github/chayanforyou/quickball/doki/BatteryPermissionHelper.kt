package io.github.chayanforyou.quickball.doki

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import java.util.Locale


object BatteryPermissionHelper {

    /* HTC */
    private val BRAND_HTC = "htc"
    private val PACKAGE_HTC_MAIN = "com.htc.pitroad"
    private val PACKAGE_HTC_COMPONENT = "com.htc.pitroad.landingpage.activity.LandingPageActivity"

    /* HUAWEI */
    private val BRAND_HUAWEI = "huawei"
    private val PACKAGE_HUAWEI_MAIN = "com.huawei.systemmanager"
    private val PACKAGE_HUAWEI_ACTION = "huawei.intent.action.HSM_PROTECTED_APPS"

    /* LETV */
    private val BRAND_LETV = "letv"
    private val PACKAGE_LETV_MAIN = "com.letv.android.letvsafe"
    private val PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.BackgroundAppManageActivity"

    /* MEIZU */
    private val BRAND_MEIZU = "meizu"
    private val PACKAGE_MEIZU_MAIN = "com.meizu.safe"
    private val PACKAGE_MEIZU_COMPONENT = "com.meizu.safe.powerui.PowerAppPermissionActivity"
    private val PACKAGE_MEIZU_ACTION = "com.meizu.power.PowerAppKilledNotification"

    /* Oppo */
    private val BRAND_OPPO = "oppo"
    private val PACKAGE_OPPO_MAIN = "com.coloros.oppoguardelf"
    private val PACKAGE_OPPO_COMPONENT = "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity"
    private val PACKAGE_OPPO_COMPONENT_FALLBACK = "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"

    /* Vivo */
    private val BRAND_VIVO = "vivo"
    private val PACKAGE_VIVO_MAIN = "com.vivo.abe"
    private val PACKAGE_VIVO_FALLBACK = "com.iqoo.powersaving"
    private val PACKAGE_VIVO_COMPONENT = "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
    private val PACKAGE_VIVO_COMPONENT_FALLBACK = "com.iqoo.powersaving.PowerSavingManagerActivity"

    /* Samsung */
    private val BRAND_SAMSUNG = "samsung"
    private val PACKAGE_SAMSUNG_MAIN = "com.samsung.android.lool"
    private val PACKAGE_SAMSUNG_FALLBACK = "com.samsung.android.sm_cn"
    private val PACKAGE_SAMSUNG_COMPONENT = "com.samsung.android.sm.battery.ui.BatteryActivity"
    private val PACKAGE_SAMSUNG_COMPONENT_FALLBACK = "com.samsung.android.sm.ui.battery.BatteryActivity"
    private val PACKAGE_SAMSUNG_ACTION = "com.samsung.android.sm.ACTION_BATTERY"

    /* Xiaomi */
    private val BRAND_XIAOMI = "xiaomi"
    private val BRAND_XIAOMI_POCO = "poco"
    private val BRAND_XIAOMI_REDMI = "redmi"
    private val PACKAGE_XIAOMI_MAIN = "com.miui.powerkeeper"
    private val PACKAGE_XIAOMI_FALLBACK = "com.miui.securitycenter"
    private val PACKAGE_XIAOMI_COMPONENT = "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
    private val PACKAGE_XIAOMI_COMPONENT_FALLBACK = "com.miui.powercenter.legacypowerrank.PowerDetailActivity"

    /* ZTE */
    private val BRAND_ZTE = "zte"
    private val PACKAGE_ZTE_MAIN = "com.zte.heartyservice"
    private val PACKAGE_ZTE_COMPONENT = "com.zte.heartyservice.setting.ClearAppSettingsActivity"

    private val PACKAGES_TO_CHECK_FOR_PERMISSION = listOf(
        PACKAGE_HTC_MAIN,
        PACKAGE_HUAWEI_MAIN,
        PACKAGE_LETV_MAIN,
        PACKAGE_MEIZU_MAIN,
        PACKAGE_OPPO_MAIN,
        PACKAGE_VIVO_MAIN,
        PACKAGE_VIVO_FALLBACK,
        PACKAGE_SAMSUNG_MAIN,
        PACKAGE_SAMSUNG_FALLBACK,
        PACKAGE_XIAOMI_MAIN,
        PACKAGE_XIAOMI_FALLBACK,
        PACKAGE_ZTE_MAIN
    )

    private val Context.applicationName: String
        get() {
            val applicationInfo = this.applicationInfo
            val stringId = applicationInfo.labelRes
            return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
            else this.getString(stringId)
        }

    /**
     * It will attempt to open the specific manufacturer settings screen with the autostart permission
     * If [open] is changed to false it will just check the screen existence
     *
     * @param context
     * @param open, if true it will attempt to open the activity, otherwise it will just check its
     *   existence
     * @param newTask, if true when the activity is attempted to be opened it will add
     *   FLAG_ACTIVITY_NEW_TASK to the intent
     * @return true if the activity was opened or is confirmed that it exists (depending on [open]]),
     *   false otherwise
     */
    fun getPermission(context: Context, open: Boolean = true, newTask: Boolean = true): Boolean {
        try {
            return when (Build.BRAND.lowercase(Locale.ROOT)) {
                BRAND_HTC -> startForHtc(context, open, newTask)
                BRAND_HUAWEI -> startForHuawei(context, open, newTask)
                BRAND_MEIZU -> startForMeizu(context, open, newTask)
                BRAND_OPPO -> startForOppo(context, open, newTask)
                BRAND_VIVO -> startForVivo(context, open, newTask)
                BRAND_SAMSUNG -> startForSamsung(context, open, newTask)
                BRAND_XIAOMI,
                BRAND_XIAOMI_POCO,
                BRAND_XIAOMI_REDMI -> startForXiaomi(context, open, newTask)
                BRAND_ZTE -> startForZte(context, open, newTask)
                BRAND_LETV -> startForLetv(context, open, newTask)
                else -> startDefault(context, open, newTask)
            }
        } catch (_: Exception) {
            return false
        }
    }

    private fun startForHtc(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return start(
            context,
            listOf(PACKAGE_HTC_MAIN),
            listOf(getIntent(PACKAGE_HTC_MAIN, PACKAGE_HTC_COMPONENT, newTask)),
            open)
    }

    private fun startForHuawei(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return startFromAction(
            context, listOf(getIntentFromAction(PACKAGE_HUAWEI_ACTION, newTask)), open)
    }

    private fun startForLetv(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return start(
            context,
            listOf(PACKAGE_LETV_MAIN),
            listOf(getIntent(PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT, newTask)),
            open)
    }

    private fun startForMeizu(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return start(
            context,
            listOf(PACKAGE_MEIZU_MAIN),
            listOf(getIntent(PACKAGE_MEIZU_MAIN, PACKAGE_MEIZU_COMPONENT, newTask)),
            open) ||
                startFromAction(context, listOf(getIntentFromAction(PACKAGE_MEIZU_ACTION, newTask)), open)
    }

    private fun startForOppo(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return if (start(
                context,
                listOf(PACKAGE_OPPO_MAIN),
                listOf(
                    getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT, newTask),
                    getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT_FALLBACK, newTask)),
                open))
            true
        else launchAppInfo(context, open, newTask)
    }

    private fun startForVivo(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return if (start(
                context,
                listOf(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_FALLBACK),
                listOf(
                    getIntent(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT, newTask),
                    getIntent(PACKAGE_VIVO_FALLBACK, PACKAGE_VIVO_COMPONENT_FALLBACK, newTask)),
                open))
            true
        else launchAppInfo(context, open, newTask)
    }

    private fun startForSamsung(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return startFromAction(
            context, listOf(getIntentFromAction(PACKAGE_SAMSUNG_ACTION, newTask)), open) ||
                start(
                    context,
                    listOf(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_FALLBACK),
                    listOf(
                        getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT, newTask),
                        getIntent(PACKAGE_SAMSUNG_FALLBACK, PACKAGE_SAMSUNG_COMPONENT_FALLBACK, newTask)),
                    open)
    }

    private fun startForXiaomi(context: Context, open: Boolean, newTask: Boolean): Boolean {
        val extras = mapOf(
            "package_name" to context.packageName,
            "package_label" to context.applicationName)
        return start(
            context,
            listOf(PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_FALLBACK),
            listOf(
                getIntentWithExtras(PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_COMPONENT, extras, newTask),
                getIntentWithExtras(PACKAGE_XIAOMI_FALLBACK, PACKAGE_XIAOMI_COMPONENT_FALLBACK, extras, newTask)),
            open)
    }

    private fun startForZte(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return start(
            context,
            listOf(PACKAGE_ZTE_MAIN),
            listOf(getIntent(PACKAGE_ZTE_MAIN, PACKAGE_ZTE_COMPONENT, newTask)),
            open)
    }

    private fun startDefault(context: Context, open: Boolean, newTask: Boolean): Boolean {
        val dozeModeIntent = getActionDozeMode(context, newTask)
        return if (dozeModeIntent != null) startFromAction(context, listOf(dozeModeIntent), open)
        else false
    }

    /**
     * Checks whether the autostart permission is present in the manufacturer and supported by the
     * helper
     *
     * @param context
     * @param onlyIfSupported if true, the method will only return true if the screen is supported by
     *   the helper. If false, the method will return true as long as the permission exist even if the
     *   screen is not supported by the helper.
     * @return true if autostart permission is present in the manufacturer and supported by the
     *   helper, false otherwise
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun isBatterySaverPermissionAvailable(
        context: Context,
        onlyIfSupported: Boolean = false
    ): Boolean {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)

        // Check OEM packages
        for (packageInfo in packages) {
            if (PACKAGES_TO_CHECK_FOR_PERMISSION.contains(packageInfo.packageName) &&
                (!onlyIfSupported || getPermission(context, open = false)))
                return true
        }

        // Fallback: check doze mode for unknown/default brands
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }

        return false
    }

    private fun getActionDozeMode(context: Context, newTask: Boolean): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val ignoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName)
            if (!ignoringBatteryOptimizations) {
                return try {
                    @SuppressLint("BatteryLife")
                    getIntentFromAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, newTask).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                } catch (_: Exception) {
                    // Fallback: open general battery optimization list
                    getIntentFromAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, newTask)
                }
            }
        }
        return null
    }

    private fun launchAppInfo(context: Context, open: Boolean, newTask: Boolean): Boolean {
        return try {
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            i.data = "package:${context.packageName}".toUri()
            if (open) {
                context.startActivity(i)
                true
            } else {
                isActivityFound(context, i)
            }
        } catch (exx: Exception) {
            exx.printStackTrace()
            false
        }
    }

    @Throws(Exception::class)
    private fun startIntent(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (exception: Exception) {
            exception.printStackTrace()
            throw exception
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun isPackageExists(context: Context, targetPackage: String): Boolean {
        val packages: List<ApplicationInfo>
        val pm = context.packageManager
        packages = pm.getInstalledApplications(0)
        for (packageInfo in packages) {
            if (packageInfo.packageName == targetPackage) {
                return true
            }
        }
        return false
    }

    /**
     * Generates an intent with the passed package and component name
     *
     * @param packageName
     * @param componentName
     * @param newTask
     * @return the intent generated
     */
    private fun getIntent(packageName: String, componentName: String, newTask: Boolean): Intent {
        return Intent().apply {
            component = ComponentName(packageName, componentName)
            if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Generates an intent with the passed package and component name
     *
     * @param packageName
     * @param componentName
     * @param newTask
     * @return the intent generated
     */
    private fun getIntentWithExtras(
        packageName: String,
        componentName: String,
        extras: Map<String, Any?>,
        newTask: Boolean
    ): Intent {
        return Intent().apply {
            component = ComponentName(packageName, componentName)
            if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            extras.forEach { (key, value) ->
                when (value) {
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is Float -> putExtra(key, value)
                    is Double -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Bundle -> putExtra(key, value)
                    null -> putExtra(key, null as String?)
                }
            }
        }
    }

    /**
     * Generates an intent with the passed action
     *
     * @param intentAction
     * @param newTask
     * @return the intent generated
     */
    private fun getIntentFromActionWithExtras(
        intentAction: String,
        newTask: Boolean,
        extras: Map<String, String>
    ): Intent {
        return Intent().apply {
            action = intentAction
            if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            extras.forEach { entry -> this@apply.putExtra(entry.key, entry.value) }
        }
    }

    /**
     * Generates an intent with the passed action
     *
     * @param intentAction
     * @param newTask
     * @return the intent generated
     */
    private fun getIntentFromAction(intentAction: String, newTask: Boolean): Intent {
        return Intent().apply {
            action = intentAction
            if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Will query the passed intent to check whether the Activity really exists
     *
     * @param context
     * @param intent, intent to open an activity
     * @return true if activity is found, false otherwise
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun isActivityFound(context: Context, intent: Intent): Boolean {
        val flag: Int = PackageManager.MATCH_DEFAULT_ONLY
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flag.toLong()))
        } else {
            context.packageManager.queryIntentActivities(intent, flag)
        }.isNotEmpty()
    }

    /**
     * Will query the passed list of intents to check whether any of the activities exist
     *
     * @param context
     * @param intents, list of intents to open an activity
     * @return true if activity is found, false otherwise
     */
    private fun areActivitiesFound(context: Context, intents: List<Intent>): Boolean {
        return intents.any { isActivityFound(context, it) }
    }

    /**
     * Will attempt to open the AutoStart settings activity from the passed list of intents in order.
     * The first activity found will be opened.
     *
     * @param context
     * @param intents list of intents
     * @return true if an activity was opened, false otherwise
     */
    private fun startScreen(context: Context, intents: List<Intent>): Boolean {
        intents.forEach {
            if (isActivityFound(context, it)) {
                startIntent(context, it)
                return@startScreen true
            }
        }
        return false
    }

    /**
     * Will trigger the common permission logic. If [open] is true it will attempt to open the
     * specific manufacturer setting screen, otherwise it will just check for its existence
     *
     * @param context
     * @param packages, list of known packages of the corresponding manufacturer
     * @param intents, list of known intents that open the corresponding manufacturer settings screens
     * @param open, if true it will attempt to open the settings screen, otherwise it just check its
     *   existence
     * @return true if the screen was opened or exists, false if it doesn't exist or could not be
     *   opened
     */
    private fun start(
        context: Context,
        packages: List<String>,
        intents: List<Intent>,
        open: Boolean
    ): Boolean {
        return if (packages.any { isPackageExists(context, it) }) {
            if (open) startScreen(context, intents) else areActivitiesFound(context, intents)
        } else false
    }

    /**
     * Will trigger the common permission logic. If [open] is true it will attempt to open the
     * specific manufacturer setting screen, otherwise it will just check for its existence
     *
     * @param context
     * @param intentActions, list of known intent actions that open the corresponding manufacturer
     *   settings screens
     * @param open, if true it will attempt to open the settings screen, otherwise it just check its
     *   existence
     * @return true if the screen was opened or exists, false if it doesn't exist or could not be
     *   opened
     */
    private fun startFromAction(
        context: Context,
        intentActions: List<Intent>,
        open: Boolean
    ): Boolean {
        return if (open) startScreen(context, intentActions)
        else areActivitiesFound(context, intentActions)
    }
}