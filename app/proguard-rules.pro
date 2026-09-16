# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/tyamada22/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom keep rules here that are specific to your project.

# PDFBox-Android optional dependencies
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
