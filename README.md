[![Build Status](https://travis-ci.org/brescia123/sKanner.svg?branch=master)](https://travis-ci.org/brescia123/sKanner)
[![](https://jitpack.io/v/brescia123/sKanner.svg)](https://jitpack.io/#brescia123/sKanner)

# Skanner (WIP)

Skanner is an Android library written in Kotlin that uses [OpenCV for Android](http://opencv.org/platforms/android.html) to scan a document within a given image.

## Download

To include Skanner into your app using gradle just add [JitPack](https://jitpack.io/) repository to your root `build.gradle`
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
and add the dependency to your app `build.gradle`
```groovy
compile 'com.github.brescia123:skanner:x.x.x'
```
replacing x.x.x with the [last release](https://github.com/brescia123/skanner/releases) version.

## Multi APK support

Since Skanner uses OpenCV that is written in C++ it has to include its native libraries compiled for every architecture.
The supported architectures are:

- arm64-v8a
- armeabi-v7a
- mips
- mips64
- x86
- x86_64

You can reduce considerably the APK of your application by taking advantage of [Apk Splits](https://developer.android.com/studio/build/configure-apk-splits.html) function of the 
Android Gradle plugin. You just have to add these lines to your `build.gradle` file:

```groovy
android {
    ...
    splits {
        abi {
            enable true
            reset()
            include 'arm64-v8a', 'armeabi-v7a', 'mips', 'mips64', 'x86', 'x86_64'
            universalApk false
        }
    }
}

```

Check also the [documentation](https://developer.android.com/google/play/publishing/multiple-apks.html) 
regarding publishing multiple APKs to the Play Store.
