package com.fprieto.hms.wearable.credentials

import com.fprieto.hms.wearable.BuildConfig

class CredentialsProvider() {
    fun getPeerPkgName() = BuildConfig.peerPkgName
    fun getPeerFingerPrint() = BuildConfig.peerFingerPrint
}