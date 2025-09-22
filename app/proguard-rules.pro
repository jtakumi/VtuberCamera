# VtuberCamera ProGuard Rules
# Android 15 (API 35) 対応版

# =============================================================================
# 基本設定
# =============================================================================

# デバッグ情報を保持（スタックトレース用）
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature

# リフレクション用の情報を保持
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# =============================================================================
# CameraX関連
# =============================================================================

# CameraXのクラスを保護
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Camera2 API関連
-keep class android.hardware.camera2.** { *; }
-dontwarn android.hardware.camera2.**

# =============================================================================
# Jetpack Compose関連
# =============================================================================

# Composeランタイムを保護
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }

# Compose内部クラスの警告を抑制
-dontwarn androidx.compose.**

# =============================================================================
# ViewModel & LiveData
# =============================================================================

# ViewModelを保護
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# LiveDataを保護
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# =============================================================================
# Firebase関連
# =============================================================================

# Firebase全般を保護
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Analytics
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.android.gms.measurement.**

# =============================================================================
# Kotlin関連
# =============================================================================

# Kotlinx Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }

# Kotlin Reflection
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# =============================================================================
# Android 15対応
# =============================================================================

# Android 15の新しいAPIに関する警告を抑制
-dontwarn android.os.Build$VERSION_CODES
-dontwarn android.permission.**

# パーシャルフォトアクセス関連
-keep class android.provider.MediaStore$** { *; }
-dontwarn android.provider.MediaStore$**

# Private Space関連（将来の対応）
-dontwarn android.os.UserHandle
-dontwarn android.content.pm.PackageManager$**

# =============================================================================
# 外部ライブラリ
# =============================================================================

# Coil (画像読み込み)
-keep class coil.** { *; }
-dontwarn coil.**

# AndroidX全般
-keep class androidx.** { *; }
-dontwarn androidx.**

# =============================================================================
# ARCore関連
# =============================================================================

# ARCore SDKを保護
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**

# ARCore Session関連
-keep class com.google.ar.core.Session { *; }
-keep class com.google.ar.core.Frame { *; }
-keep class com.google.ar.core.Camera { *; }
-keep class com.google.ar.core.Pose { *; }
-keep class com.google.ar.core.Anchor { *; }

# =============================================================================
# Filament 3Dレンダリングエンジン関連
# =============================================================================

# Filamentエンジンを保護
-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**

# Filament JNI関連
-keep class com.google.android.filament.Engine { *; }
-keep class com.google.android.filament.Renderer { *; }
-keep class com.google.android.filament.Scene { *; }
-keep class com.google.android.filament.Camera { *; }
-keep class com.google.android.filament.View { *; }

# GLTF IO関連
-keep class com.google.android.filament.gltfio.** { *; }
-dontwarn com.google.android.filament.gltfio.**

# =============================================================================
# VRM関連
# =============================================================================

# VRMカスタム実装を保護
-keep class com.example.vtubercamera.vrm.** { *; }
-dontwarn com.example.vtubercamera.vrm.**

# VRMモデル関連のカスタムクラス
-keep class com.example.vtubercamera.data.VRMModel { *; }
-keep class com.example.vtubercamera.data.VRMLoader { *; }
-keep class com.example.vtubercamera.data.VRMParser { *; }

# VRM表情・ポーズデータ
-keepclassmembers class com.example.vtubercamera.data.Expression { *; }
-keepclassmembers class com.example.vtubercamera.data.Pose { *; }

# =============================================================================
# 数学ライブラリ関連
# =============================================================================

# JOML (Java OpenGL Math Library)
-keep class org.joml.** { *; }
-dontwarn org.joml.**

# 3D変換関連のクラス
-keep class org.joml.Vector3f { *; }
-keep class org.joml.Quaternionf { *; }
-keep class org.joml.Matrix4f { *; }

# =============================================================================
# JSON処理関連
# =============================================================================

# Gson (VRMメタデータ用)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# VRMメタデータクラスのシリアライゼーション
-keepclassmembers class com.example.vtubercamera.data.VRMMetadata { *; }
-keepclassmembers class com.example.vtubercamera.data.Expression { *; }
-keepclassmembers class com.example.vtubercamera.data.Pose { *; }

# =============================================================================
# アプリケーション固有
# =============================================================================

# プロジェクト固有のクラスを保護
-keep class com.example.vtubercamera.** { *; }

# Dataクラスのフィールドを保護
-keepclassmembers class com.example.vtubercamera.** {
    *;
}

# =============================================================================
# パフォーマンス最適化
# =============================================================================


# 最適化を有効化
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# =============================================================================
# デバッグ支援（リリース時はコメントアウト推奨）
# =============================================================================

# クラス名を保持（デバッグ用）
# -keepnames class ** { *; }

# メソッド名を保持（デバッグ用）
# -keepclassmembernames class ** { *; }