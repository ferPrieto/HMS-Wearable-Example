# HMS-Wearable-Example :iphone: :hearts: :watch:

A companion Android app that interacts with a Huawei SmartWatch(HarmonyOS).

## Setup
1. Credentials

It's necessary to add the specific values for the next entries in the `local.properties`:
* **keyAlias**=XXX
* **keyPassword**=YYY
* **storePassword**=ZZZ
* **peerPkgName**="com.your.package"
* **peerFingerprint**="com.your.package_FINGER_PRINT="

More info about how to get the fingerPrint value here :point_right: [fingerprint]

2. AGC JSON File

`agconnect-services.json` is needed too. More info about how to get this file here  :point_right: [agconnect-services.json]

3. Apply for Health Kit

In order to be able to communicate with Huawei Health and get any available tracked data, it's necessary to apply from the Huawei Developer console, more info here: [healthkit-application]

4. Apply for Wear Engine

In order to be able to communicate with a Huawei Smart Watch, it will be necessary to apply from the Huawei Developer console, more info here: [wearengine-application]

## Demo

<p align="center">
  <img src="art/Demo-SpaceX.gif">
</p>


## License

    Copyright 2021 Fernando Prieto Moyano

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[fingerprint]: https://developer.huawei.com/consumer/en/doc/development/connectivity-Guides/fitnesswatch-send-message-0000001052460491#EN-US_TOPIC_0000001074076988__section1361217411408
[agconnect-services.json]: https://developer.huawei.com/consumer/de/doc/development/AppGallery-connect-Guides/agc-get-started
[healthkit-application]: https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/apply-kitservice-0000001050071707
[wearengine-application]: https://developer.huawei.com/consumer/en/doc/development/connectivity-Guides/applying-wearengine-0000001050777982