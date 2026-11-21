package com.example.minierp.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.minierp.db.DB
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** كتابة نسخة احتياطية JSON كاملة وإرجاع Uri جاهز للمشاركة/الحفظ. */
fun exportBackup(context: Context): Uri? = try {
    val json = DB.exportJson()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    val dir = File(context.filesDir, "documents/backups").apply { mkdirs() }
    val file = File(dir, "backup_$stamp.json")
    file.writeText(json)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (e: Exception) {
    null
}

/** قراءة ملف JSON واستبدال قاعدة البيانات بمحتواه. */
fun importBackup(context: Context, uri: Uri): Boolean = try {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    if (text.isNullOrBlank()) {
        false
    } else {
        DB.importJson(text)
        true
    }
} catch (e: Exception) {
    false
}
