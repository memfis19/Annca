# Annca
Android solution to simplify work with different camera apis. Incude video and photo capturing.

## Example of using
```
 AnncaConfiguration.Builder builder = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
 new Annca(builder.build()).launchCamera();
```
and thats it. You can use more extended settings i.e.
```
 AnncaConfiguration.Builder videoLimited = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
 videoLimited.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO);
 videoLimited.setMediaQuality(AnncaConfiguration.MEDIA_QUALITY_AUTO);
 videoLimited.setVideoFileSize(5 * 1024 * 1024);
 new Annca(videoLimited.build()).launchCamera();
```
in this example you request video capturing with file size limited to 5Mb and auto quality to record video which meet this requirements.
