package io.github.chayanforyou.quickball.ui.screens.home.components

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.billing.TipJarClient

/**
 * Support sheet for the Google Play build: one-time tips through Play Billing.
 */
@Composable
fun SupportBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    val tipJar = remember {
        TipJarClient(context) {
            Toast.makeText(context, R.string.tip_thank_you, Toast.LENGTH_LONG).show()
            currentOnDismiss()
        }
    }

    DisposableEffect(tipJar) {
        tipJar.connect()
        onDispose { tipJar.close() }
    }

    SupportSheetLayout(
        description = stringResource(R.string.support_tip_description),
        onDismissRequest = onDismissRequest
    ) {
        when (tipJar.status) {
            TipJarClient.Status.LOADING -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            TipJarClient.Status.UNAVAILABLE -> Text(
                text = stringResource(R.string.tip_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            TipJarClient.Status.READY -> tipJar.tips.forEach { tip ->
                SupportRow(
                    title = tip.name,
                    subtitle = tip.description,
                    onClick = { activity?.let { tipJar.launchPurchase(it, tip) } }
                ) {
                    PricePill(price = tip.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty())
                }
            }
        }
    }
}

/**
 * Price shown as a filled pill, so it reads as the buy button. The whole row handles the tap.
 */
@Composable
private fun PricePill(price: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = price,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
