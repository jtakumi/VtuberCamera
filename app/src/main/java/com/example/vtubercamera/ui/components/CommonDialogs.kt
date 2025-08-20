package com.example.vtubercamera.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.vtubercamera.R

/**
 * 汎用的な確認ダイアログコンポーネント
 * @param onDismiss ダイアログを閉じる処理
 * @param onConfirm 確認ボタンが押された時の処理
 * @param title ダイアログのタイトル
 * @param text ダイアログの本文
 * @param confirmText 確認ボタンのテキスト
 * @param dismissText キャンセルボタンのテキスト
 */
@Composable
fun ConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "確認",
    dismissText: String = "キャンセル"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * 部分アクセス権限ダイアログ
 */
@Composable
fun PartialAccessDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    ConfirmationDialog(
        onDismiss = onDismiss,
        onConfirm = {
            // 設定画面を開く
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
        title = stringResource(R.string.photo_access),
        text = stringResource(R.string.partial_access_message),
        confirmText = stringResource(R.string.open_settings),
        dismissText = stringResource(R.string.later)
    )
}

/**
 * 写真削除確認ダイアログ（汎用コンポーネントを使用）
 */
@Composable
fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmationDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = stringResource(R.string.delete_photo_title),
        text = stringResource(R.string.delete_photo_message),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel)
    )
}

@Preview(locale = "ja")
@Preview(locale = "en")
@Composable
fun DeleteConfirmDialogPreview(){
    DeleteConfirmDialog(
        onDismiss = {},
        onConfirm = {}
    )
}

@Preview(locale = "ja")
@Preview(locale = "en")
@Composable
fun PartialAccessDialogPreview(){
    PartialAccessDialog(
        onDismiss = {},
        context = androidx.compose.ui.platform.LocalContext.current
    )
}
