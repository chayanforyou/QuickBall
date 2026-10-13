package io.github.chayanforyou.quickball.ui.screens.home.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

// bKash wallet number shown in the sheet; the row is hidden while this is blank
private const val BKASH_NUMBER = "01734811761"

// Account type and the bKash menu option the sender must use, shown as in the bKash app
private const val BKASH_ACCOUNT_TYPE = "Personal"
private const val BKASH_METHOD = "Send Money"

@OptIn(ExperimentalMaterial3Api::class)
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

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.support_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.support_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SupportPlatforms.forEach { platform ->
                    SupportRow(
                        title = platform.name,
                        subtitle = platform.host,
                        trailingIcon = painterResource(R.drawable.ic_arrow_forward),
                        onClick = { openPlatform(platform) }
                    )
                }

                // bKash has no payment link, so tapping copies the number
                if (BKASH_NUMBER.isNotBlank()) {
                    SupportRow(
                        title = "bKash ($BKASH_ACCOUNT_TYPE)",
                        subtitle = "$BKASH_NUMBER · $BKASH_METHOD",
                        trailingIcon = rememberVectorPainter(Icons.Rounded.ContentCopy),
                        trailingDescription = stringResource(R.string.copy_number),
                        onClick = { copyBkashNumber() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportRow(
    title: String,
    subtitle: String,
    trailingIcon: Painter,
    onClick: () -> Unit,
    trailingDescription: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_coffee),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = trailingIcon,
                contentDescription = trailingDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
