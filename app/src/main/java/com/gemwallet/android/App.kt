package com.gemwallet.android

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import dagger.hilt.android.HiltAndroidApp
import java.lang.System


@HiltAndroidApp
class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        walletConnectConfig()
    }

    private fun walletConnectConfig() {
//        val projectId = "238a413d92737d557e4383160caa8a72"
//        val projectId = "3bc07cd7179d11ea65335fb9377702b6"
        val projectId = "0d9db544461f12ed6dd8450a6c717753"
        val connectionType = ConnectionType.AUTOMATIC
        val metaData = Core.Model.AppMetaData(
            name = "Gem Wallet",
            description = "Gem Web3 Wallet",
            url = "https://gemwallet.com",
            icons = listOf("https://gemwallet.com/images/gem-logo-256x256.png"),
            redirect = "gem://wc/"
        )
        CoreClient.initialize(
            application = this,
            projectId = projectId,
            metaData = metaData,
            connectionType = connectionType,
            telemetryEnabled = false,
        ) {
            Log.d("WalletConnect", "Err", it.throwable)
        }
        val initParams = Wallet.Params.Init(core = CoreClient)
        WalletKit.initialize(initParams) { _ -> }
    }

    companion object {
        init {
            System.loadLibrary("TrustWalletCore")
            System.loadLibrary("gemstone")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512Mb
                    .build()
            }
            .build()
    }
}