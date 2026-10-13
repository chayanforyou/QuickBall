package io.github.chayanforyou.quickball.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Play Billing wrapper for one-time tips.
 *
 * Tips are consumable: every purchase is consumed right away, which also acknowledges it,
 * so the same tip can be bought again and Google does not refund it as unacknowledged.
 */
class TipJarClient(
    context: Context,
    private val onTipReceived: () -> Unit
) : PurchasesUpdatedListener {

    enum class Status { LOADING, READY, UNAVAILABLE }

    companion object {
        val TIP_PRODUCT_IDS = listOf("tip_small", "tip_medium", "tip_large")
    }

    var status by mutableStateOf(Status.LOADING)
        private set

    var tips by mutableStateOf<List<ProductDetails>>(emptyList())
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryTips()
                    consumeOwnedTips()
                } else {
                    status = Status.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                if (tips.isEmpty()) status = Status.UNAVAILABLE
            }
        })
    }

    fun close() {
        billingClient.endConnection()
    }

    fun launchPurchase(activity: Activity, tip: ProductDetails) {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(tip)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingResponseCode.OK) return
        purchases?.forEach { consume(it, notify = true) }
    }

    private fun queryTips() {
        val products = TIP_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        billingClient.queryProductDetailsAsync(params) { result, response ->
            val found = response.productDetailsList
                .filter { it.oneTimePurchaseOfferDetails != null }
                .sortedBy { it.oneTimePurchaseOfferDetails?.priceAmountMicros }

            tips = found
            status = if (result.responseCode == BillingResponseCode.OK && found.isNotEmpty()) {
                Status.READY
            } else {
                Status.UNAVAILABLE
            }
        }
    }

    /**
     * Consume tips that were paid while the app was closed, such as completed pending payments.
     */
    private fun consumeOwnedTips() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                purchases.forEach { consume(it, notify = false) }
            }
        }
    }

    private fun consume(purchase: Purchase, notify: Boolean) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()

        billingClient.consumeAsync(params) { result, _ ->
            if (notify && result.responseCode == BillingResponseCode.OK) {
                mainHandler.post(onTipReceived)
            }
        }
    }
}
