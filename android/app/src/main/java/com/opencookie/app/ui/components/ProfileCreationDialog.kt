package com.opencookie.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.opencookie.app.R

@Composable
fun ProfileCreationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OpenCookieConfirmDialog(
        title = stringResource(R.string.profile_create_confirm_title),
        message = stringResource(R.string.profile_create_confirm_message),
        confirmText = stringResource(R.string.profile_create_confirm),
        dismissText = stringResource(R.string.profile_create_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
