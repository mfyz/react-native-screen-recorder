# mfyz's Notes/Tweaks

### Changes/Tweaks over the original repo

- Android build target and build tools are updated to more recent versions (except gradle)
- Movie recording target changed from External SD to temp cache folder/file. (I will use the recorded mp4 to convert to gif frames using ffmpeg on top of this repo). In order to find the recorded file, in your emulator, Open Android Studio > View > Tool Windows > Device File Explorer. In the tool window navigate to /data/data/com.screenrecorder/cache (or files) folders. See mp4 files after each recording. You can right click and save file to your desktop for further inspection.
- Playback was/is not working after the recording (but it's successfully saving the recording. Check file explorer).
- If you get bundler/server errors that you can't get react build to be bundled for debug builds, you can skip it to manually bundle your JS app with running `npm run bundle:android` command. It will create bundle file in /android/app/src/main/assets folder. Make sure to re-bundle when making JS changes.

Note: Just Android is tested. iOS not tested! But I implemented screen recroding using same library to iOS on RN in the past.

-----

# Screen Recorder
This project is set up using [create-react-native-app](https://github.com/react-community/create-react-native-app) by facebook.

## Objective
To enable react native app to make use of native modules, in this case, screen recording module.

## Accomplishement
The screen recording feature is able to run on both platforms but the implementation is quite different on each platform.

### iOS
[ASScreenRecorder](https://github.com/alskipp/ASScreenRecorder) is included as an external library to handle the screen recording function. All I need to do is to create `RecorderManager` which exports the "start" and "stop" module functions for react-native app to call.

The output video will be saved into iOS camera roll, therefore `CameraRoll` module (which is available from `react-native`) is used to retrieve the video*.

_* In this app, it's naively assumed that the recording is always the latest video in the camara roll._

### Android
_There seems no libraries similar to ASScreenRecorder on Android and due to the lack of experience on native app development, I have to heavily rely on other people's [work](#References) to come out with a solution for Android._

To implement screen recording on Android, multiple classes from multiple android packages have to be imported, such as `MediaProjection`, `VirtualDisplay`, `MediaRecorder` and etc. The implementation details are mainly in `MainActivity.java`.

The output video is stored in external storage of Android for the time being (that means it may not work on devices without external storage). Unlike the implementation on iOS which generate a new copy of video file for each recording session, the same video file will be overwritten again and again on Android with current implementation.


#### Video playing
Initially since there's only one copy of the video all the time, the file path to the video is deterministic, therefore, a package called `react-native-fs` is used to obtain the full path of the video from the device and supply to the `VideoPlayer` component to play.

After exploring ["Sending Events to JavaScript"](https://facebook.github.io/react-native/docs/native-modules-ios.html#sending-events-to-javascript), the file path of the video is sent back to react side by emitting respective event. Therefore, `react-native-fs` is no longer needed and the react side also does not need to memorize the file path anymore.


### References
http://www.truiton.com/2015/05/capture-record-android-screen-using-mediaprojection-apis/

https://github.com/vinceyuan/ReactRecorder

https://medium.com/jamesob-com/recording-your-android-screen-7e0e75aae260

https://medium.com/@wenchihhsieh/how-to-record-screen-in-android-ece7eff6bb77


## How to run this project locally
Run `npm install` to install all the dependencies.


## Available Scripts
In the project directory, you can run:

### `npm run ios`
Open the app in the iOS Simulator if you're on a Mac and have it installed.

### `npm run android`
Open the app on a connected Android device or emulator.
