# VRMファイルのインポート方法 / How to Import VRM Files

## 日本語

### VRMアバターをインポートする手順

1. **AR カメラ画面を開く**
   - アプリを起動すると、AR カメラ画面が表示されます
   
2. **アバターライブラリにアクセス**
   - アバターが読み込まれていない場合、画面中央に「No Avatar Loaded」というメッセージが表示されます
   - 「Open Avatar Library」ボタンをタップします
   - または、画面上部の人アイコン（👤）をタップします

3. **VRMファイルを選択**
   - アバターライブラリ画面で、画面右下の「+」ボタンをタップします
   - または、画面上部の「+」アイコンをタップします
   - デバイスのファイル選択画面が開きます
   - 端末に保存されているVRMファイルを選択します

4. **インポート完了**
   - 選択したVRMファイルが自動的にインポートされます
   - インポートが成功すると、アバターライブラリにアバターが表示されます
   - アバターをタップして選択できます

### 対応ファイル形式
- `.vrm` ファイル (VRM 1.0 および VRM 0.0)
- ファイルサイズ上限: 100MB

### トラブルシューティング

**ファイルが見つからない場合:**
- VRMファイルがデバイスの適切な場所に保存されていることを確認してください
- ファイルマネージャーアプリで確認できます

**インポートに失敗する場合:**
- ファイルが破損していないか確認してください
- ファイルサイズが100MB以下であることを確認してください
- ストレージの空き容量を確認してください

**権限エラーが表示される場合:**
- アプリの設定でストレージアクセス権限が有効になっていることを確認してください

---

## English

### Steps to Import VRM Avatar

1. **Open AR Camera Screen**
   - Launch the app to see the AR Camera screen
   
2. **Access Avatar Library**
   - If no avatar is loaded, you'll see a "No Avatar Loaded" message in the center
   - Tap the "Open Avatar Library" button
   - Alternatively, tap the person icon (👤) in the top app bar

3. **Select VRM File**
   - In the Avatar Library screen, tap the "+" FAB button in the bottom right
   - Or tap the "+" icon in the top app bar
   - The device file picker will open
   - Select a VRM file stored on your device

4. **Import Complete**
   - The selected VRM file will be imported automatically
   - Upon successful import, the avatar will appear in your library
   - Tap the avatar to select and use it

### Supported File Formats
- `.vrm` files (VRM 1.0 and VRM 0.0)
- Maximum file size: 100MB

### Troubleshooting

**If you can't find your file:**
- Ensure the VRM file is stored in an accessible location on your device
- Use a file manager app to verify the file location

**If import fails:**
- Verify the file is not corrupted
- Ensure the file size is under 100MB
- Check your device has sufficient storage space

**If you see permission errors:**
- Verify storage access permissions are enabled in the app settings

---

## スクリーンショット / Screenshots

### 1. AR カメラ画面 / AR Camera Screen
```
┌─────────────────────────────┐
│ ← AR Camera                 │
│                             │
│    ┌───────────────┐        │
│    │ No Avatar     │        │
│    │ Loaded        │        │
│    │               │        │
│    │ [Open Avatar  │        │
│    │  Library]     │        │
│    └───────────────┘        │
│                             │
└─────────────────────────────┘
```

### 2. アバターライブラリ画面 / Avatar Library Screen
```
┌─────────────────────────────┐
│ ← Avatar Library      + ⟳   │
├─────────────────────────────┤
│ Total: 0 | Favorites: 0     │
├─────────────────────────────┤
│                             │
│    No avatars in library    │
│    Import VRM files to      │
│    get started              │
│                             │
│      [+ Import Avatar]      │
│                             │
│                             │
│                        (+)  │
└─────────────────────────────┘
```

### 3. ファイル選択画面 / File Picker
```
┌─────────────────────────────┐
│ Select a file               │
├─────────────────────────────┤
│ 📁 Downloads                │
│   📄 my_avatar.vrm          │
│   📄 character.vrm          │
│                             │
│ 📁 Documents                │
│                             │
│ 📁 VRM Files                │
│                             │
└─────────────────────────────┘
```
