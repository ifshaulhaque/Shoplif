package com.ifsha.shoplif.screens.onboarding.mobile_no_screen

import android.Manifest
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ifsha.shoplif.Constants.MOBILE_VERIFICATION_EXCEED_TIME_SEC
import com.ifsha.shoplif.Constants.SMS_CHECK_DELAY_SEC
import com.ifsha.shoplif.R
import com.ifsha.shoplif.components.CustomButton
import com.ifsha.shoplif.components.CustomTextField
import com.ifsha.shoplif.components.OnboardingScreen
import com.ifsha.shoplif.components.PermissionsHandler
import com.ifsha.shoplif.screens.onboarding.OnboardingRoute
import com.ifsha.shoplif.screens.onboarding.OnboardingViewModel
import com.ifsha.shoplif.ui.theme.ShoplifTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun MobileNoScreen(
    navController: NavController,
    onboardingViewModel: OnboardingViewModel? = hiltViewModel()
) {
    val context = LocalContext.current
    var remainingVerificationTime by remember {
        mutableIntStateOf(60)
    }

    var mobileNo by remember {
        mutableStateOf("")
    }
    var errorText by remember {
        mutableStateOf<String?>(null)
    }
    val invalidText = stringResource(id = R.string.invalid)
    var showVerifyingDialog by remember {
        mutableStateOf(false)
    }

    PermissionsHandler(
        permissions = listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS),
        rationale = stringResource(id = R.string.sms_rationale),
        onAllPermissionsGranted = {},
        onPermissionsDenied = {},
        onDismissRationale = {
            (context as Activity).finish()
        }
    )

    OnboardingScreen(
        image = R.drawable.auth_sticker,
        middleContent = {
            Column {
                Text(
                    text = stringResource(id = R.string.welcome),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(text = stringResource(id = R.string.need_mobile_no_on_same_device))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomTextField(
                value = mobileNo,
                onValueChanged = {
                    if (it.length == 10) errorText = null
                    if (it.length <= 10) mobileNo = it
                },
                errorText = errorText,
                label = stringResource(id = R.string.mobile_no),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )

            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.send_sms),
                onClick = {
                    if (mobileNo.length != 10) {
                        errorText = invalidText
                    } else {
                        val otp = Random.nextInt(100000, 1000000).toString()
//                        val otp = "000000"
                        onboardingViewModel?.sendSMS(mobileNo, otp)

                        showVerifyingDialog = true
                        var timmer = MOBILE_VERIFICATION_EXCEED_TIME_SEC

                        CoroutineScope(Dispatchers.Main).launch {
                            while (timmer > 0) {
                                delay(1000)
                                timmer -= 1
                                remainingVerificationTime = timmer
                                if (timmer % SMS_CHECK_DELAY_SEC == 0) {
                                    val sms = onboardingViewModel?.readLastSmsFromNumber(mobileNo, context)
                                    Toast.makeText(context, sms.toString(), Toast.LENGTH_SHORT).show()
                                    if (!sms.isNullOrEmpty() && sms == otp) {
                                        showVerifyingDialog = false
                                        navController.navigate(route = OnboardingRoute.NAME_SCREEN)
                                        cancel()
                                    }
                                }
                            }

                            showVerifyingDialog = false
                            Toast.makeText(context, context.getString(R.string.mobile_no_verification_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    if (showVerifyingDialog) {
        AlertDialog(
            onDismissRequest = {
                showVerifyingDialog = false
            }, confirmButton = { },
            title = {
                Text(text = stringResource(id = R.string.verifying))
            },
            text = {
                Text(
                    text = stringResource(id = R.string.verify_time_alert, remainingVerificationTime),
                    fontSize = 18.sp
                )
            },
            properties = DialogProperties(dismissOnClickOutside = false)
        )
    }
}

@Composable
@Preview(showBackground = true)
fun MobileNoScreenPreview() {
    ShoplifTheme {
        MobileNoScreen(navController = rememberNavController())
    }
}