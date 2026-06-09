package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import com.example.sampleapp.ui.theme.SampleAppTheme
import com.zetexa.app_sdk.pigeon.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pre-warm the FlutterEngine once and register Pigeon APIs
        if (FlutterEngineCache.getInstance().get("zetsim_engine") == null) {
            val flutterEngine = FlutterEngine(applicationContext).apply {
                dartExecutor.executeDartEntrypoint(
                    DartExecutor.DartEntrypoint.createDefault()
                )
            }
            val messenger = flutterEngine.dartExecutor.binaryMessenger

            NativeBrandConfigApi.setUp(messenger, object : NativeBrandConfigApi {
                override fun getInitialBrandTheme(): BrandConfig {
                    return BrandConfig(
                        resellerId = "1c4e1a36-c94c-45bb-8e63-402091a2d093",
                        resellerEmail = "zetexa@zetexa.com",
                        resellerName = "ZetSIM",
                        preferredCurrency = "USD",
                        environment = BrandEnvironment.STG,
                        theme = BrandThemeConfig(
                            colors = BrandColorConfig(
                                primary = "#0000FF",
                                secondary = "#00FF00",
                                tertiary = "#FF0000",
                                success = "#00FF00",
                                error = "#FF0000",
                                warning = "#FFA500",
                                background = "#FFFFFF",
                                secondaryBackground = "#F0F0F0"
                            ),
                            typography = BrandTypographyConfig(
                                headingFontFamily = "Roboto",
                                bodyFontFamily = "Roboto"
                            )
                        )
                    )
                }
            })

            HostToFlutterCheckoutApi.setUp(messenger, object : HostToFlutterCheckoutApi {
                override fun getNativeHostCheckoutContext(): NativeHostCheckoutContextMessage {
                    return NativeHostCheckoutContextMessage(
                        nativeHostUserSignedIn = false,
                        nativePrefillProfile = NativeCheckoutPrefillProfile(
                            nativePrefillFirstName = "John",
                            nativePrefillLastName = "Doe",
                            nativePrefillEmail = "john.doe@example.com",
                            nativePrefillPhone = "+1234567890",
                            nativePrefillNationalityIso2 = "US"
                        )
                    )
                }
            })

            FlutterEngineCache.getInstance().put("zetsim_engine", flutterEngine)
        }

        setContent {
            SampleAppTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = FlutterFragmentActivity
                                    .CachedEngineIntentBuilder(ZetSdkActivity::class.java, "zetsim_engine")
                                    .destroyEngineWithActivity(false)
                                    .build(this@MainActivity)
                                startActivity(intent)
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Blue,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "ZetSDK (Default)")
                        }

                        SdkNavigationDestination.entries.forEach { destination ->
                            Button(
                                onClick = {
                                    val intent = FlutterFragmentActivity
                                        .CachedEngineIntentBuilder(ZetSdkActivity::class.java, "zetsim_engine")
                                        .destroyEngineWithActivity(false)
                                        .build(this@MainActivity)
                                        .apply {
                                            putExtra("destination", destination.name)
                                        }
                                    startActivity(intent)
                                }, colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.DarkGray,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = destination.name.replace("_", " "))
                            }
                        }
                    }
                }
            }
        }
    }
}