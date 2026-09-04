# The downloader library invokes parts of its Java/native bridge dynamically.
# Its bundled consumer rules cover most cases; these keep the public bridge and
# native entry points stable while R8 removes unused UI/icon code.
-keep class com.yausername.youtubedl_android.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
