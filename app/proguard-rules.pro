# Add project specific R8 rules here.
# Room and Compose ship their own keep rules; only classes reached by name
# (reflection, Room entities whose columns are named after their fields) need
# an entry here.

# API DTOs are decoded with kotlinx.serialization. The library ships rules for
# generated serializers; this keeps the generic page wrappers' companions too.
-keepclassmembers @kotlinx.serialization.Serializable class fr.mandarine.environmentquality.data.remote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
