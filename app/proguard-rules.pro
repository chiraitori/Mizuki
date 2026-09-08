# The downloader library invokes parts of its Java/native bridge dynamically.
# Its bundled consumer rules cover most cases; these keep the public bridge and
# native entry points stable while R8 removes unused UI/icon code.
-keep class com.yausername.youtubedl_android.** { *; }
# ZIP fields are instantiated reflectively while unpacking the Python runtime.
-keep class * implements org.apache.commons.compress.archivers.zip.ZipExtraField {
    public <init>();
}
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
