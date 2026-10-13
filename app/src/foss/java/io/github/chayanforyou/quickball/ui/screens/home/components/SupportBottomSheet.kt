package io.github.chayanforyou.quickball.ui.screens.home.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.chayanforyou.quickball.R

private class SupportPlatform(val name: String, val host: String, val url: String)

private val SupportPlatforms = listOf(
    SupportPlatform(
        name = "Gumroad",
        host = "gumroad.com",
        url = "https://chayanforyou.gumroad.com/coffee"
    ),
    SupportPlatform(
        name = "Ko-fi",
        host = "ko-fi.com",
        url = "https://ko-fi.com/chayanforyou"
    )
)

// bKash wallet number shown in the sheet
private const val BKASH_NUMBER = "01734811761"

private const val BKASH_ACCOUNT_TYPE = "Personal"
private const val BKASH_METHOD = "Send Money"

/**
 * Support sheet for builds outside Google Play: opens external donation pages.
 */
@Composable
fun SupportBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    fun openPlatform(platform: SupportPlatform) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, platform.url.toUri()))
        } catch (_: ActivityNotFoundException) {
            // No browser available
        }
        onDismissRequest()
    }

    fun copyBkashNumber() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("bKash", BKASH_NUMBER))

        // Android 13+ shows its own clipboard confirmation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.number_copied, Toast.LENGTH_SHORT).show()
        }
    }

    SupportSheetLayout(
        description = stringResource(R.string.support_description),
        onDismissRequest = onDismissRequest
    ) {
        SupportPlatforms.forEach { platform ->
            SupportRow(
                title = platform.name,
                subtitle = platform.host,
                onClick = { openPlatform(platform) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // bKash has no payment link, so tapping copies the number
        if (BKASH_NUMBER.isNotBlank()) {
            SupportRow(
                title = "bKash ($BKASH_ACCOUNT_TYPE)",
                subtitle = "$BKASH_NUMBER · $BKASH_METHOD",
                onClick = { copyBkashNumber() }
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.copy_number),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
