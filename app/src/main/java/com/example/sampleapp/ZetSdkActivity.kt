package com.example.sampleapp

import android.os.Bundle
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngineCache
import com.zetexa.app_sdk.pigeon.NativeNavigationHostApi
import com.zetexa.app_sdk.pigeon.FlutterNavigationApi
import com.zetexa.app_sdk.pigeon.SdkNavigationDestination

class ZetSdkActivity : FlutterFragmentActivity(), NativeNavigationHostApi {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register this activity instance as the navigation listener for dismiss calls
        val engine = FlutterEngineCache.getInstance().get("zetsim_engine")
        if (engine != null) {
            val messenger = engine.dartExecutor.binaryMessenger
            NativeNavigationHostApi.setUp(messenger, this)
            
            // Navigate to the target page if destination extra is present
            intent.getStringExtra("destination")?.let { destName ->
                try {
                    val destination = SdkNavigationDestination.valueOf(destName)
                    FlutterNavigationApi(messenger).navigateTo(destination) { result ->
                        if (result.isFailure) {
                            android.util.Log.e("ZetSdkActivity", "Navigation failed", result.exceptionOrNull())
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    android.util.Log.e("ZetSdkActivity", "Invalid destination: $destName")
                }
            }
        }
    }

    override fun dismissFlutter() {
        finish()
    }

    override fun onDestroy() {
        // Clear the handler reference to avoid memory leaks
        val engine = FlutterEngineCache.getInstance().get("zetsim_engine")
        if (engine != null) {
            NativeNavigationHostApi.setUp(engine.dartExecutor.binaryMessenger, null)
        }
        super.onDestroy()
    }
}
