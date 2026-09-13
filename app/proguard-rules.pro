# 9Router Monitor Proguard Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
