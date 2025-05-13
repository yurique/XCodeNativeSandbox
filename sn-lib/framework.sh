#!/usr/bin/env bash

rm -rf NativeSandboxLib.xcframework/

xcodebuild -create-xcframework \
                    -library modules/native-bridge-ios/target/scala-3.6.3/libnativebridgeios.a \
                    -headers ./include/ \
                    -library modules/native-bridge-simulator/target/scala-3.6.3/libnativebridgesimulator.a \
                    -headers ./include/ \
                    -output NativeSandboxLib.xcframework
