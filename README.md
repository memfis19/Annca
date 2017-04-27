# Annca

[ ![Download](https://api.bintray.com/packages/m-e-m-f-i-s/io.github.memfis19/annca/images/download.svg) ](https://bintray.com/m-e-m-f-i-s/io.github.memfis19/annca/_latestVersion)[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Annca-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4814)  [![Build Status](https://travis-ci.org/memfis19/Annca.svg?branch=master)](https://travis-ci.org/memfis19/Annca) [![API](https://img.shields.io/badge/API-10%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=10)

Android solution to simplify work with different camera apis. Include video and photo capturing features with possibility to select quality for appropriate media action etc. In current solution were used some approaches from <a href="https://github.com/google/grafika">Grafika project</a> and <a href="https://github.com/googlesamples/android-Camera2Video">Google camera samples</a>.

<img src="https://github.com/memfis19/Annca/blob/master/art/default_camera.png" width="200px" /> <img src="https://github.com/memfis19/Annca/blob/master/art/settings_for_video_limitation.png" width="200px" /><img src="https://github.com/memfis19/Annca/blob/master/art/video_camera.png" width="200" /><img src="https://github.com/memfis19/Annca/blob/master/art/video_low_quality.png" width="200" />

## Forks
Some `Annca`'s forks that can be useful if current functionality is not enought:<br>
* <a href="https://github.com/florent37/CameraFragment">CameraFragment</a>

## Example of using
Add Annca activities to the your app manifest file:
```
<activity
        android:name="io.github.memfis19.annca.internal.ui.camera.Camera1Activity"
        android:screenOrientation="portrait"
        android:theme="@style/ThemeFullscreen" />
<activity
        android:name="io.github.memfis19.annca.internal.ui.camera2.Camera2Activity"
        android:screenOrientation="portrait"
        android:theme="@style/ThemeFullscreen" />
<activity
        android:name="io.github.memfis19.annca.internal.ui.preview.PreviewActivity"
        android:screenOrientation="portrait"
        android:theme="@style/ThemeFullscreen" />
```
#### Please note that `android:screenOrientation="portrait"` is mandatory. In current implementation device rotation is detecting via sensor.

Don't forget to put permissions or request them:
```
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
A code example of the most simple using:
```
 AnncaConfiguration.Builder builder = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
 new Annca(builder.build()).launchCamera();
```
and thats it. You can use more extended settings i.e.:
```
 AnncaConfiguration.Builder videoLimited = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
 videoLimited.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO);
 videoLimited.setMediaQuality(AnncaConfiguration.MEDIA_QUALITY_AUTO);
 videoLimited.setVideoFileSize(5 * 1024 * 1024);
 videoLimited.setMinimumVideoDuration(5 * 60 * 1000);
 new Annca(videoLimited.build()).launchCamera();
```
in this example you request video capturing which is limited by file size for 5Mb and predefined minimum video duration for 5 min. To achieve this result Annca will decrease video bit rate, so use it carefully to avoid unexpected result (i.e. low quality).

##### How to get result?
```
 @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == CAPTURE_MEDIA && resultCode == RESULT_OK) {
          String filePath = data.getStringExtra(AnncaConfiguration.Arguments.FILE_PATH);
      }
   }
```

## How to add to your project?
```
compile 'io.github.memfis19:annca:0.3.7'
```
## How to customize camera?
By customizing I mean change all avaliable camera controls(not camera preview) to take photos, video recording, quality settings and more in future. To be able to customize camera view you should create activity and extend it via ```AnncaCameraActivity<T>```, where ```T``` is camera id type (due to that in camera 1 api camera ids represent as int values and in camera 2 api as string values). You should implement all requested methods. For more details please look at ```BaseAnncaActivity``` at library project. In future I will provide more detailed instructions how to do it, but anyway it is quite easy task. In case if you gave some troubles or questions, please use <a href="https://github.com/memfis19/Annca/issues">GitHub Issues</a>.
#### Short summary:
-Extend your activity from ```AnncaCameraActivity<T>```;
##### -Declare it in the manifest only in Portrait mode.
-Override all methods;</br>
-For method ```createCameraController``` use ```Camera1Controller``` or ```Camera2Controller``` respectively;</br>
-If you want to rotate your views do it inside ```onScreenRotation(int degrees)``` method;</br>
-For setting your camera contol layout use method ```getUserContentView()```;</br>
-If you pass some parameters to your camera via bundle please use method ```onProcessBundle(Bundle savedInstanceState)``` which will be called before ```getUserContentView()```;</br>
-In case you need to retrieve some data from camera controller or manager use in or after method ```onCameraControllerReady()``` was called.

## Square camera
Sample app contains example of using square camera. Few words about it:
1. Before appearing <a href="https://developer.android.com/reference/android/media/MediaCodec.html">MediaCodec</a> and <a href="https://developer.android.com/reference/android/media/MediaMuxer.html">MediaMuxer</a> there were no ways to record video from specific surface (at least with native tools) that's why in current example I'm using view resizing to achieve square preview. Look at ```updateCameraPreview``` method within ```SquareCameraActivity```. 
2. To make square photo I'm cropping origin, please look at ```onPhotoTaken``` method within ```SquareCameraActivity```.
3. To make square video I'm cropping origin with ffmpeg library, please look at ```onVideoRecordStop``` method within ```SquareCameraActivity```.
Conclusion: Becasue of some android limitations at least before API level 18 it is not possible to record square video. That's why to make solution more less generic I'm using crop outputs approach. In future I'll implement separate solution for API level 18+ to make possible apply more custom settings for camera (shaders, custom photo/preview/video sizes without cropping etc.).

## Know issue
Library has not release yet, so it contains some issues.

## Roadmap
-Improve determinig camera quality settings;</br>
-Extend annca configuration settings;</br>
-Add fragments supporting;</br>
-Add opengl supporting.</br>

## Bugs and Feedback
For bugs, feature requests, and discussion please use <a href="https://github.com/memfis19/Annca/issues">GitHub Issues</a>.

# [LICENSE](/LICENSE.md)

###### MIT License

###### Copyright (c) 2016 Rodion Surzhenko

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
