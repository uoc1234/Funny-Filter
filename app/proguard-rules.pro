# ==== Android Core ====
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod

# ==== Gson ====
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class * implements com.google.gson.InstanceCreator
-keep class * extends com.google.gson.TypeAdapterFactory
-keepattributes Signature
-keepattributes *Annotation*

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==== Moshi ====
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
}
-keep class kotlin.Metadata { *; }
-dontwarn javax.annotation.**

# ==== Retrofit ====
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# ==== OkHttp ====
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okhttp3.CertificatePinner { *; }
-keep class okhttp3.internal.tls.** { *; }
-keep class okhttp3.internal.platform.** { *; }
-dontwarn okhttp3.**

# ==== SSL Providers ====
-keep class org.bouncycastle.jsse.** { *; }
-keep class org.bouncycastle.jsse.provider.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.openjsse.** { *; }
-keep class org.openjsse.javax.net.ssl.** { *; }
-keep class org.openjsse.net.ssl.** { *; }

# ==== Jetpack & Kotlin ====
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.LiveData { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ==== Facebook SDK ====
-keep class com.facebook.** { *; }

# ==== API & Model Classes ====
-keep class com.titanbbl.funny.face.filter.game.model.** { *; }
-keep class com.titanbbl.funny.face.filter.game.model.api.** { *; }

# ==== CRITICAL: Fix ParameterizedType ClassCastException ====
# Keep all classes in the game package to prevent generic type issues
-keep class com.titanbbl.funny.face.filter.game.** { *; }
-keep interface com.titanbbl.funny.face.filter.game.** { *; }

# Keep specific model classes that might be used in generic operations
-keep class com.titanbbl.funny.face.filter.game.model.VideoItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.ChallengeItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.GuideModel { *; }
-keep class com.titanbbl.funny.face.filter.game.model.FaceSetToken { *; }
-keep class com.titanbbl.funny.face.filter.game.model.GuessItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.PhysicalFeatureItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.LipFallItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.CelebrityMatch { *; }
-keep class com.titanbbl.funny.face.filter.game.model.VideoFilterItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.Resource { *; }
-keep class com.titanbbl.funny.face.filter.game.model.GameItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.CelebrityInfo { *; }

# Keep API response models
-keep class com.titanbbl.funny.face.filter.game.model.api.MusicResponse { *; }
-keep class com.titanbbl.funny.face.filter.game.model.api.VideoResponse { *; }
-keep class com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem { *; }
-keep class com.titanbbl.funny.face.filter.game.model.api.Song { *; }

# Keep Repository implementations
-keep class com.titanbbl.funny.face.filter.game.data.repository.VideoRepository { *; }
-keep class com.titanbbl.funny.face.filter.game.data.repository.MusicRepository { *; }
-keep class com.titanbbl.funny.face.filter.game.data.repository.PredictionRepository { *; }
-keep class com.titanbbl.funny.face.filter.game.data.repository.UserDetailsRepository { *; }
-keep class com.titanbbl.funny.face.filter.game.data.repository.UserListRepository { *; }

# Keep API service interfaces
-keep interface com.titanbbl.funny.face.filter.game.data.api.MusicApiService { *; }
-keep interface com.titanbbl.funny.face.filter.game.data.api.VideoApiService { *; }
-keep interface com.titanbbl.funny.face.filter.game.data.api.PredictionApiService { *; }

# Keep generic type information for collections and API calls
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Exceptions

# Keep all classes that might be used in generic operations
-keep class * implements java.io.Serializable { *; }
-keep class * implements java.lang.Cloneable { *; }

# Keep all classes in the data package
-keep class com.titanbbl.funny.face.filter.game.data.** { *; }
-keep interface com.titanbbl.funny.face.filter.game.data.** { *; }

# Keep all classes in the api package
-keep class com.titanbbl.funny.face.filter.game.model.api.** { *; }
-keep interface com.titanbbl.funny.face.filter.game.model.api.** { *; }

# Keep generic type information for Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep all classes with @SerializedName annotation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==== Dontwarn cho compile-time references ====
-dontwarn javax.lang.model.**
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

-keep class androidx.lifecycle.LiveData.**{*; }
 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

 # R8 full mode strips generic signatures from return types if not kept.
 -if interface * { @retrofit2.http.* public * *(...); }
 -keep,allowoptimization,allowshrinking,allowobfuscation class <3>

 # With R8 full mode generic signatures are stripped for classes that are not kept.
 -keep,allowobfuscation,allowshrinking class retrofit2.Response