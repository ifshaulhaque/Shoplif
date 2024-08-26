package com.ifsha.shoplif.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ifsha.shoplif.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@Composable
fun PermissionHandler(
    permission: String,
    rationale: String,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onDismissRationale: () -> Unit = {}
) {
    val context = LocalContext.current
    val showRationaleDialog = remember { mutableStateOf(false) }
    val showSettingsDialog = remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            if (!shouldShowRequestPermissionRationale(context, permission)) {
                showSettingsDialog.value = true
            } else {
                onPermissionDenied()
                showRationaleDialog.value = true
            }
        }
    }

    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(context, permission)) {
            PermissionChecker.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            PermissionChecker.PERMISSION_DENIED -> {
                if (shouldShowRequestPermissionRationale(context, permission)) {
                    showRationaleDialog.value = true
                } else {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }

    if (showRationaleDialog.value) {
        RationaleDialog(
            rationale = rationale,
            onDismiss = {
                showRationaleDialog.value = false
                onDismissRationale.invoke()
            },
            onConfirm = { requestPermissionLauncher.launch(permission) }
        )
    }

    if (showSettingsDialog.value) {
        SettingsDialog(
            rationale = rationale,
            onDismiss = {
                showSettingsDialog.value = false
                onDismissRationale.invoke()
            },
            onOpenSettings = { openAppSettings(context) }
        )
    }
}

@Composable
fun PermissionsHandler(
    permissions: List<String>,
    rationale: String,
    onAllPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit,
    onDismissRationale: () -> Unit = {}
) {
    val context = LocalContext.current
    val showRationaleDialog = remember { mutableStateOf(false) }
    val showSettingsDialog = remember { mutableStateOf(false) }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsResult ->
        val allPermissionsGranted = permissionsResult.all { it.value }
        if (allPermissionsGranted) {
            onAllPermissionsGranted()
        } else {
            val permanentlyDenied = permissions.any { !shouldShowRequestPermissionRationale(context, it) }
            if (permanentlyDenied) {
                showSettingsDialog.value = true
            } else {
                onPermissionsDenied()
                showRationaleDialog.value = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            onAllPermissionsGranted()
        } else {
            val showRationale = permissions.any { shouldShowRequestPermissionRationale(context, it) }
            if (showRationale) {
                showRationaleDialog.value = true
            } else {
                requestPermissionsLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    if (showRationaleDialog.value) {
        RationaleDialog(
            rationale = rationale,
            onDismiss = {
                showRationaleDialog.value = false
                onDismissRationale.invoke()
            },
            onConfirm = { requestPermissionsLauncher.launch(permissions.toTypedArray()) }
        )
    }

    if (showSettingsDialog.value) {
        SettingsDialog(
            rationale = rationale,
            onDismiss = {
                showSettingsDialog.value = false
                onDismissRationale.invoke()
            },
            onOpenSettings = { openAppSettings(context) }
        )
    }
}


@Composable
fun RationaleDialog(
    rationale: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.permission_required)) },
        text = { Text(text = rationale) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.no_thanks))
            }
        }
    )
}

@Composable
fun SettingsDialog(
    rationale: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.permission_required)) },
        text = { Text(text = rationale) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = stringResource(id = R.string.settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.no_thanks))
            }
        }
    )
}

fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
    return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
