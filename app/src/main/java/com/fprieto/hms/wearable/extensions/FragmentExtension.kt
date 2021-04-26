package com.fprieto.hms.wearable.extensions

import com.fprieto.hms.wearable.mapper.RemoteDataMessageToLocalMapper
import com.huawei.wearengine.p2p.Message

fun Message.toLocalData() = RemoteDataMessageToLocalMapper().toLocalDataMessage(this)