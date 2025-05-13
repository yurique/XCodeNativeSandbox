## build the scala native lib

```shell
cd sn-lib
sbt linkLibraries; buildFramework
```

This will have created the framework in the `sn-lib/NativeSandboxLib.xcframework` directory.
This framework is referenced from the xcode project.

## xcode project config file

Create/edit the config override file: `NativeSandbox/ConfigOverride.xcconfig`

```
DEVELOPER_TEAM = MY_TEAM_ID
```

(*might* work with a random string instead of a real team id, at least in simulator)

## open the xcode project

Open the `NativeSandbox` project directory in XCode.

Run the application, click the button.