Created 2 todos

I'll first scan the codebase for large or meaningful commented-out sections, then summarize what features they correspond to.

Searched text for `//` (`**/app/src/main/java/**`), 200 results

Searched text for `TODO` (`**/app/src/main/java/**`), 52 results

Starting (2/2) *Summarize commented-out features*

このリポジトリをざっとスキャンした結果、「コメントアウトされていて無効化されている機能」と「まだ実装されていないプレースホルダー機能（TODO）」が混在している状態でした。  
ご質問は「コメントアウトされている機能は何か？」なので、**実際にコードとして無効化されている／未実装の機能**に絞って整理します。

## 1. Filamentベースの本格ARレンダリング機能

vrm 以下の多くのクラスで、Filamentを使った本格的なARレンダリング処理が「コメントのみ／TODOのみ」で、実コードはまだ入っていません。

代表的には:

- ARSceneManager.kt
  - Filamentシーンの生成  
    - `// TODO: Create Filament scene when dependencies are available`
  - ライト（太陽光・環境光）の生成と更新  
    - `// TODO: Create Filament light entities`
    - `// TODO: Setup IBL and skybox when Filament is available`
    - `// TODO: Apply lighting changes to Filament scene`
    - `// TODO: Update Filament light entities with new values`
  - VRMアバターエンティティとの連携  
    - `// TODO: Create avatar entity from VRM model`
    - `// TODO: Apply transform to avatar entity`
    - `// TODO: Remove avatar entity from scene`
  - カメラ設定  
    - `// TODO: Configure Filament camera`
    - `// TODO: Cleanup Filament scene resources`

- FilamentARRenderer.kt
  - Filamentエンジンの初期化・描画パイプライン
    - `// TODO: Initialize Filament engine when dependencies are available`
    - `// TODO: Render the scene`
    - `// TODO: Cleanup Filament resources`
  - ARフレームからカメラ・ライティング・キャプチャへの反映
    - `// TODO: Update AR camera with frame data`
    - `// TODO: Apply lighting to Filament scene`
    - `// TODO: Capture frame from Filament renderer`
  - VRMメッシュへのブレンドシェイプやボーン変形
    - `// TODO: Apply blend shapes to mesh`
    - `// TODO: Apply bone transforms`
  - `// TODO: Filament engine methods (when dependencies are available)` とあるように、Filamentの実処理はまだコメントベースの設計のみ。

- FilamentTextureManager.kt
  - テクスチャ圧縮
    - `// TODO: Implement ETC2 compression`
    - `// TODO: Implement ASTC compression`
  - それ以外はプレーンなBitmap→RGBA変換などの最低限のみで、本格的なGPU向け最適化は未実装（コメントのみ）。

- ShadowSystem.kt
  - 影用フレームバッファとシャドウマップ描画
    - `// TODO: Create actual Filament framebuffer when dependencies are available`
    - `// TODO: Actual shadow map rendering when Filament is available`
    - `// TODO: Cleanup Filament framebuffer resources`

- VRMFilamentConverter.kt
  - VRM/GLTFの実際のパース処理
    - `// TODO: Implement actual VRM/GLTF parsing`

- ARCameraConfig.kt
  - Filamentカメラへの実投影・ビューマトリクス反映
    - `// TODO: Configure Filament camera when available`
    - `// TODO: Apply matrices to Filament camera`
    - `// TODO: Apply projection and view matrices to Filament camera`
    - `// TODO: Implement proper ray casting when Filament is available`
    - `// TODO: Implement proper world to screen projection`

**まとめると**  
このプロジェクトは「Filament を使った本格ARレンダラー」を前提に設計されていますが、**Filament依存部分の多くはコメント（TODO）や説明のみで、実装コードはまだコメントアウト／未実装の状態**です。  
その結果、現在はより簡易な描画・管理で動かしつつ、将来的にFilamentを差し替える構成になっています。

## 2. VRM関連の詳細実装の一部

- VRMRepositoryImpl.kt
  - 冒頭付近や末尾に「This is a placeholder implementation」「In a real implementation, you would:」といったコメントがあり、
    - VRMファイルの詳細パース
    - アバターメタデータの永続化
  がコメントベースで設計されているだけで、**実装は簡易版**に留まっています。

- VRMModel.kt
  - コメントで「Create basic bone mapping」「Would be populated from actual animation data」などとあり、
    - 本格的なボーンマッピング
    - アニメーション／ポーズの詳細
  はまだ仕様コメントで、完全には実装されていません。

これらは「コメントで仕様や将来の振る舞いを書いているが、本当にやりたい高機能版はまだコードになっていない」という意味で、**高機能VRM処理の一部が“コメント状態の機能”**になっています。

## 3. UIまわりのコメント（無効化というより説明）

CameraScreen.kt などUI側には:

- `// Lens switching indicators and feedback`
- `// Lens switch hint (shown when switching is available)`
- `// Lens switch feedback overlay`
- `// Auto-hide lens switch feedback after delay`

といったコメントがありますが、ここは「機能そのものがコメントアウトされている」というより、**すでに実装済みのUIの説明コメント**です。  
`//` 行だけが見えても、周囲のコードは動いているため、「無効化中の機能」ではありません。

## 4. その他のコメント・TODO

- FilamentShaderManager.kt などでも
  - `// TODO: Actual Filament shader compilation when dependencies are available`
- README的な README_AR_RENDERER.md
  - `- TODO markers for Filament-specific implementations`

これらは**ドキュメントレベルのコメント**で、プロジェクト全体として「Filament依存機能はまだコメント/TODOとして残している」という状況を補足しています。

---

## 要約

このプロジェクトで「コメントアウト／TODOとして残されている主な機能」は、ざっくり次の通りです。

1. **Filamentを用いた本格ARレンダリング機能**
   - シーン構築、ライト、IBL/スカイボックス、シャドウ、カメラ制御、VRMモデル描画、ブレンドシェイプやボーンスキニング、フレームキャプチャなど。
   - 多くが ARSceneManager.kt, FilamentARRenderer.kt, ShadowSystem.kt, FilamentTextureManager.kt, ARCameraConfig.kt, VRMFilamentConverter.kt にコメント/TODOとして書かれている。

2. **VRMファイルのフル機能パース・管理**
   - 詳細なVRM/GLTFパース、メタデータの永続化や高度なライブラリ管理などの一部がプレースホルダ実装＋コメント状態。

3. **UIのコメントは主に説明であり、機能の無効化ではない**
   - CameraScreen.kt のレンズ切替のコメントなどは、既にある機能の説明コメントに近い。

もし「どのファイルのどのコメントが“とりあえず止めている機能”なのか」をもっと細かく一覧化したい場合は、特定ファイル（例: CameraScreen.kt や FilamentARRenderer.kt）を指定してもらえれば、そのファイル単位で詳細に洗い出します。