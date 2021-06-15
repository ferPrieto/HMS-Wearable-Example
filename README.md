# HMS-Wearable-Example

A companion Android app that interacts with a Huawei SmartWatch(HarmonyOS).

## Setup
###Credentials
It's necessary to add the specific values for the next entries in the `local.properties`:
* **keyAlias**=XXX
* **keyPassword**=YYY
* **storePassword**=ZZZ
* **peerPkgName**="com.your.package"
* **peerFingerprint**="com.your.package_FINGER_PRINT="

More info about how to get the fingerPrint value here :point_right: [fingerprint]

###AGC JSON File
`agconnect-services.json` is needed too. More info about how to get this file here  :point_right: [agconnect-services.json]

[fingerprint]: https://developer.huawei.com/consumer/en/doc/development/connectivity-Guides/fitnesswatch-send-message-0000001052460491#EN-US_TOPIC_0000001074076988__section1361217411408
[agconnect-services.json]: https://developer.huawei.com/consumer/de/doc/development/AppGallery-connect-Guides/agc-get-started