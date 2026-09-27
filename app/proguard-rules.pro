# ProGuard rules for Secure Pass

# Keep Gson models and serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep VaultItem data model for Gson serialization/deserialization
-keep class com.applify.securepass.data.VaultItem { *; }
