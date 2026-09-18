# Rules for the Spreva app module.
# Keep serialization serializers (looked up reflectively for polymorphic content)
-keepclassmembers class com.spreva.** {
    *** Companion;
}
-keepclasseswithmembers class com.spreva.** {
    kotlinx.serialization.KSerializer serializer(...);
}
