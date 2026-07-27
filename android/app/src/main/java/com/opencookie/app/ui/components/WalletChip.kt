package com.opencookie.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opencookie.app.R

@Composable
fun WalletChip(
    address: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val truncated = if (address.length > 8) {
        "${address.take(4)}...${address.takeLast(4)}"
    } else {
        address
    }

    Surface(
        modifier = modifier.clickable {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("wallet", address))
            Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
        },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = truncated,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
