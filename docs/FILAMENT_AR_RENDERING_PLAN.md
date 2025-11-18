# Filament を用いた本格 AR レンダリング機能 実装手順メモ

このドキュメントでは、`FilamentARRenderer` / `ARSceneManager` / `ARCameraConfig` / `ShadowSystem` / `VRMFilamentConverter` など既存クラスの設計を前提に、Filament を用いた本格 AR レンダリング機能を実装するためのおおまかな手順を整理する。

## 1. 前提とゴール

- **前提**
  - すでに ARCore を利用した AR セッション管理 (`Session`, `Frame`, `LightEstimate` など) は行われている。
  - VRM モデルのローディングやメタデータ管理は `VRMRepositoryImpl`, `VRMModel` である程度実装済み。
  - Filament 連携用のラッパークラスや構造体 (`FilamentARRenderer`, `FilamentTextureManager`, `FilamentMaterialManager`, `ShadowSystem`, `LightingSystem`, `VRMFilamentConverter` 等) は骨組みが用意されているが、Filament 依存の中身が TODO / プレースホルダになっている。

- **ゴール**
  - Filament エンジンを利用して VRM アバターを AR 空間上にレンダリングできるようにする。
  - ARCore カメラ姿勢・投影行列に同期したカメラで描画する。
  - 環境光推定 (`LightEstimate`) を使ったライティング、およびシャドウ（少なくとも簡易版）を適用する。
  - `FilamentARRenderer` が `ARRenderer` インターフェイスを満たし、UI 側から差し替え可能な構成を完成させる。

## 2. 依存関係の追加とビルド設定

1. **Filament ライブラリの導入**
   - Filament AAR or Maven Central の依存を `app/build.gradle` に追加する。
     - 例（バージョンはプロジェクト方針に合わせて選定）:
       - `com.google.android.filament:filament-android`
       - `com.google.android.filament:gltfio-android`
       - `com.google.android.filament:filament-utils-android`
   - NDK / `minSdk` / ABI 設定が Filament の要件を満たすように確認する。

2. **ProGuard / R8 設定**
   - Filament クラスが不要にシュリンクされないよう、必要であれば `proguard-rules.pro` に keep ルールを追加。

3. **ビルド検証**
   - 依存追加後に Gradle sync とビルドが通ることを確認。

## 3. Filament エンジンと基本シーンの構築

### 3.1 Filament エンジン初期化 (`FilamentARRenderer`)

`FilamentARRenderer.initialize(surface, arSession)` 内の TODO を埋める形で実装していく。

1. `Engine.create()` で Filament エンジンを生成。
2. `SwapChain` を `Engine.createSwapChain(surface)` で作成。
3. `Renderer` を `Engine.createRenderer()` で作成。
4. `Scene` と `View` を作成し、`view.scene = scene` を設定。
5. `Camera` を `Engine.createCamera()` で作成し、`view.camera = camera` を設定。
6. `view.viewport` に `viewportWidth`, `viewportHeight` を反映。
7. `LightingSystem`, `ShadowSystem` と連携するためのハンドル（ライトやシャドウマップ）を後続手順に備えて保持しておく。

### 3.2 基本シーンセットアップ (`setupBasicScene` の実装)

- 背景色・クリアカラーなど、最低限のレンダリング設定
  - `view.blendMode` や `renderer.clearOptions` の設定。
- デバッグ段階ではグリッドや単純なジオメトリ（例：床平面）を置いてレンダリング確認ができるようにする。

## 4. カメラと ARCore の統合

### 4.1 カメラ投影・ビュー行列の適用

- `ARCameraConfig` に既に以下の枠組みがある：
  - `configure(width, height, near, far)`
  - `updateWithFrame(frame)`
  - `applyCameraMatrices()` (TODO)

実装ステップ:

1. `configure` で Filament の `Camera.setProjection(...)` を呼ぶようにする。
   - 初期値として `DEFAULT_FOV` とアスペクト比を使用。
2. `updateWithFrame(frame)` で ARCore カメラから `pose`, `projectionMatrix`, `viewMatrix` を取得（既に実装済み）。
3. `applyCameraMatrices()` で Filament カメラへ行列を適用。
   - `camera.setCustomProjection(projectionMatrix, near, far)`
   - ARCore の view 行列は「カメラのワールド→ビュー変換」なので、必要に応じて逆行列を計算して `camera.setModelMatrix(...)` に渡す。
4. `FilamentARRenderer.updateFrame` 内で、毎フレーム `ARCameraConfig.updateWithFrame(frame)` を呼び出し、その後 Filament カメラの行列を更新するよう連携。

### 4.2 画面サイズ変更時のハンドリング

- `FilamentARRenderer.setViewport(width, height)` で:
  - `view.viewport` を更新。
  - `ARCameraConfig.configure(width, height, near, far)` を再呼び出し。

## 5. VRM → Filament メッシュ変換の実装強化

`VRMFilamentConverter` ではメッシュ変換の骨組みはできているが、`extractMeshData` がプレースホルダのまま。フル機能を目指す場合:

1. **VRM/GLTF パーサの導入・実装**
   - 選択肢:
     - a. 既存の glTF/VRM パーサライブラリを利用して `VRMModel.meshData` を充填する。
     - b. 自前で GLB/GLTF をパースして `MeshData` を組み立てる。
   - どちらにせよ、`MeshData` が複数メッシュ・頂点・インデックス・ボーン情報を含むように実装する。

2. **ボーン・スキニング情報の整備**
   - `Vertex` にある `boneWeights`, `boneIndices` が VRM のスキニング情報を正しく反映するようにする。
   - 将来的なシャドウ／アニメーションのために、ボーン行列配列などもレイアウトを決めておく。

3. **テクスチャ・マテリアルのマッピング**
   - `processMaterials`, `processTextures` で VRM のマテリアル名・テクスチャ名と Filament 用パラメータを対応付け。
   - PBR パラメータ (metallic, roughness, emissive) を VRM / glTF の仕様に沿って設定する。

## 6. Filament でのテクスチャ & マテリアル生成

### 6.1 テクスチャ (`FilamentTextureManager`)

1. `loadTexture(texture: FilamentTexture)` 内で:
   - `BitmapFactory` 等で `FilamentTexture.data` から Bitmap を生成。
   - 必要なら `resize` / `power-of-two` / sRGB 設定を行う（既存コメントに沿う）。
   - Filament の `Texture.Builder` と `Texture.setImage` を用いて Filament テクスチャを作成。
   - `FilamentTextureInstance` に Filament Texture ハンドルを持たせる。

2. テクスチャのライフサイクル管理
   - `clearCache()` で Filament テクスチャの `destroy` を呼ぶようにする。

### 6.2 マテリアル (`FilamentMaterialManager`)

1. Filament マテリアルの .filamat ファイル（または Ubershader）を準備し、Assets などから読み込めるようにする。
2. `createMaterial(material: FilamentMaterial, textures: Map<String, FilamentTexture>)` で:
   - `Material.Builder().build(engine)` で `Material` を生成。
   - `MaterialInstance` を作成し、`baseColor`, `metallic`, `roughness`, テクスチャサンプラなどをセット。
3. `updateLighting(materialInstance, lightingParams)` で:
   - 環境光・ディレクショナルライトの色・強度・方向などをシェーダーユニフォームに反映。

## 7. Renderable（描画オブジェクト）の生成

`FilamentARRenderer.createRenderable(mesh: FilamentMesh)` を実装していく。

1. Filament の `VertexBuffer.Builder` と `IndexBuffer.Builder` を使ってバッファを作成。
   - `VertexBuffer` に位置・法線・UV・カラー・ボーン情報のストライドとオフセットを設定。
   - `IndexBuffer` にインデックス配列をアップロード。
2. Filament の `RenderableManager.Builder` でエンティティを作成。
   - `setGeometry` でメッシュの頂点バッファ・インデックスバッファを設定。
   - `setMaterialInstanceAt` で `materialInstances` からマテリアルを割り当て。
   - 必要ならカリングやシャドウキャスト／レシーブのフラグを設定。
3. 生成したエンティティ ID を `FilamentRenderable` に保持し、`scene.addEntity(entity)` でシーンに追加。
4. `ShadowSystem` には `addShadowCaster`, `addShadowReceiver` を既に呼んでいるので、その前提で `FilamentRenderable` が Filament エンティティを参照できるようにする。

## 8. シャドウシステムの実装

`ShadowSystem` は現在、シェーダーコードやマトリクス計算の枠組みだけがある状態。

1. **Filament ベースの簡易シャドウから始める**
   - Filament 自体にシャドウ機能があるため、最初は Filament の `LightManager.Type.SUN` や `castShadows(true)` を利用し、ShadowSystem は「設定ラッパー」として最小限実装する方法もある。

2. **独自シャドウマップを使う場合の実装**
   - `createShadowMapFramebuffer()` で実際に Filament の `RenderTarget` と深度テクスチャを作成。
   - `renderShadowMap()` で:
     - シャドウ用 `View` / `Camera` を作り、ライト空間からシーンを一度レンダリングして深度を取得。
     - 結果のテクスチャをマテリアルに渡す。
   - パフォーマンスと実装コストを考えると、まずは Filament の標準シャドウで十分なケースが多い。

## 9. ライティングシステムとの連携

- `FilamentARRenderer.setLighting(lightEstimate: LightEstimate)` ではすでに:
  - `lightingSystem.updateEnvironmentLighting(lightEstimate)`
  - `shadowSystem.updateShadows(...)`
  を呼んでいる。

追加で必要になる作業:

1. `lightingSystem` が出力する `LightingParameters` をもとに Filament ライト (`LightManager`) のパラメータを更新。
   - ディレクショナルライト（太陽光）の方向・色・強度。
   - アンビエントライト（IBL or Hemispheric light）の設定。
2. `ARSceneManager` の `setupDefaultLighting`, `applyLightingChanges` の TODO を、Filament のライト操作コードで埋める。
   - もしくは `FilamentARRenderer` 側にライト管理を集約し、`ARSceneManager` はあくまでシーン状態の抽象レイヤに留める設計もあり。

## 10. 毎フレームのレンダリングループ

`FilamentARRenderer.updateFrame(frame, avatarState)` を完成させるための流れ:

1. ARCore フレームからカメラ情報を更新
   - `arCameraConfig.updateWithFrame(frame)` → `applyCameraMatrices()` → Filament カメラ更新。
2. VRM モデルが変更された場合は `loadVRMModelToScene` を呼ぶ。
3. アバターの Transform / Expression / Pose を適用
   - `applyAvatarTransform(avatarState.transform)` でエンティティのモデル行列を更新。
   - `applyExpression`, `applyPose` でシェーダーのブレンドシェイプ / ボーン行列を更新（必要に応じて追加実装）。
4. ライティングの更新
   - `setLighting` で保持した `currentLightEstimate` からライティングを更新。
5. Filament で描画
   - `renderScene()`（TODO）として、以下を実装:
     - `if (renderer.beginFrame(swapChain)) { renderer.render(view); renderer.endFrame(); }`

## 11. リソース解放とライフサイクル

- `FilamentARRenderer.cleanup()` で:
  - `shadowSystem.cleanup()` や `textureManager.clearCache()`, `materialManager.clearCache()` に加えて、
  - `engine.destroyRenderer(renderer)` / `engine.destroyView(view)` / `engine.destroyScene(scene)` / `engine.destroyCameraComponent(camera)` / `engine.destroySwapChain(swapChain)` / `Engine.destroy(engine)` など、Filament リソースの解放を行う。
- アクティビティ / フラグメントの `onPause` / `onDestroy` と連携し、ARCore のセッション停止と合わせて呼ばれるようにする。

## 12. 段階的な実装アプローチの提案

いきなり全機能を実装するのは工数が大きいため、段階的に進めるのがおすすめ。

1. **ステップ1: 単純な Filament 描画の確認**
   - Filament エンジン初期化 + カメラ投影設定 + 単色メッシュ描画までを実装。
   - ARCore 連携なしで、静的な 3D モデルが描けることを確認。

2. **ステップ2: ARCore カメラとの連携**
   - `ARCameraConfig` を通して ARCore のカメラ姿勢・投影を Filament カメラに適用。
   - 端末を動かした時に 3D モデルが AR 空間に固定されて見えることを確認。

3. **ステップ3: VRM モデルの表示**
   - `VRMFilamentConverter` で単一メッシュ + 単一マテリアル程度から実装し、徐々にスキニングや複数マテリアルへ拡張。

4. **ステップ4: ライティング & シャドウ**
   - まずは Filament 標準のディレクショナルライト + シャドウを有効にする。
   - その後、`LightingSystem` / `ShadowSystem` を通して高度なパラメータ調整やカスケードシャドウ等を検討。

5. **ステップ5: 最適化 & 品質向上**
   - テクスチャ圧縮 (ETC2 / ASTC)、LOD、カリング等を段階的に導入。
   - FPS やメモリ使用量を `PerformanceManager` 系の仕組みと連携してモニタリングする。

## 13. まとめ

- 現状のコードベースは、Filament を前提とした AR レンダリングの"設計図"がかなり詳細にコメント・クラス構成として用意されている状態。
- 実装の主な作業は、
  - Filament 依存ライブラリの導入
  - エンジン・シーン・カメラの初期化
  - VRM → Filament 変換処理の強化
  - ライティング・シャドウ・マテリアル・テクスチャの具体的な Filament API 呼び出し
  - 毎フレームのレンダリングループ実装
  に分解できる。
- 上記のステップに沿って段階的に実装すれば、リスクを抑えつつ Filament ベースの本格 AR レンダリング機能に到達できると考えられる。
