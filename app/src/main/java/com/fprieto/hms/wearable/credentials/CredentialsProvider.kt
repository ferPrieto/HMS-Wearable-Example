package com.fprieto.hms.wearable.credentials

private const val PEER_PKG_NAME = "com.fprieto.wearable"
private const val PEER_FINGERPRINT = "com.fprieto.wearable_BN1l9fz+YIXIdLKihK+1w2WBNwov+J7U3mSn78VQSCJFXi15FkWR/iQxA8PdXQBn2PIdQiABNJofoNlwYSp84KM="

class CredentialsProvider() {
    fun getPeerPkgName() = PEER_PKG_NAME
    fun getPeerFingerPrint() = PEER_FINGERPRINT
}