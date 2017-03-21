# sKanner (WIP)

sKanner is an Android library written in Kotlin that uses [OpenCV for Android](http://opencv.org/platforms/android.html) to scan a 
document within a given image.

## Multi APK support

Since sKanner uses OpenCV that is written in C++ it has to include its native libraries compiled for every architecture.
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