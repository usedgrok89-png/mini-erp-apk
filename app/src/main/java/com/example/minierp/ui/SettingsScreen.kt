@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.minierp.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minierp.db.DB
import com.example.minierp.util.exportBackup
import com.example.minierp.util.importBackup

@Composable
fun SettingsScreen(
    revision: Int,
    onChanged: () -> Unit,
    theme: String,
    onThemeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val current = remember(revision) { DB.getSettings() }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val ok = importBackup(context, uri)
            Toast.makeText(
                context,
                if (ok) "تم استيراد النسخة الاحتياطية بنجاح" else "تعذر قراءة الملف",
                Toast.LENGTH_SHORT
            ).show()
            if (ok) onChanged()
        }
    }
    var currency by remember(revision) { mutableStateOf(current.currency) }
    var shopName by remember(revision) { mutableStateOf(current.shopName) }
    var shopPhone by remember(revision) { mutableStateOf(current.shopPhone) }
    var shopAddress by remember(revision) { mutableStateOf(current.shopAddress) }
    var invoiceNote by remember(revision) { mutableStateOf(current.invoiceNote) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModernHeader(title = "الإعدادات", subtitle = "العملة، بيانات المحل، المظهر، والنسخة الاحتياطية")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إعدادات عامة", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = currency, onValueChange = { currency = it },
                    label = { Text("رمز العملة (مثال: ج.م أو $ أو ر.س)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopName, onValueChange = { shopName = it },
                    label = { Text("اسم المحل / الشركة (يظهر في الفاتورة)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopPhone, onValueChange = { shopPhone = it },
                    label = { Text("هاتف المحل (اختياري)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopAddress, onValueChange = { shopAddress = it },
                    label = { Text("عنوان المحل (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = invoiceNote, onValueChange = { invoiceNote = it },
                    label = { Text("ملاحظة تظهر في نهاية الفاتورة") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("المظهر", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = theme == "system",
                        onClick = { onThemeChanged("system") },
                        label = { Text("تبع النظام") }
                    )
                    FilterChip(
                        selected = theme == "light",
                        onClick = { onThemeChanged("light") },
                        label = { Text("نهاري ☀️") }
                    )
                    FilterChip(
                        selected = theme == "dark",
                        onClick = { onThemeChanged("dark") },
                        label = { Text("ليلي 🌙") }
                    )
                }
                Button(
                    onClick = {
                        DB.saveSettings(
                            current.copy(
                                currency = currency.trim().ifBlank { "ج.م" },
                                shopName = shopName.trim(),
                                shopPhone = shopPhone.trim(),
                                shopAddress = shopAddress.trim(),
                                invoiceNote = invoiceNote.trim(),
                                theme = theme
                            )
                        )
                        onChanged()
                        Toast.makeText(context, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("حفظ الإعدادات") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("النسخة الاحتياطية", style = MaterialTheme.typography.titleMedium)
                Text(
                    "صدّر كل بياناتك (المكونات، التركيبات، المنتجات، الفواتير، الإعدادات) في ملف واحد، واستعدها في أي وقت أو على جهاز جديد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val uri = exportBackup(context)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة النسخة الاحتياطية"))
                        } else {
                            Toast.makeText(context, "تعذر إنشاء النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تصدير النسخة الاحتياطية") }
                OutlinedButton(
                    onClick = { importLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("استيراد نسخة احتياطية") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("معلومات", style = MaterialTheme.typography.titleMedium)
                Text(
                    "• كل البيانات محفوظة على جهازك فقط ولا تُرسل لأي سيرفر.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• أسعار المكونات للعلم فقط؛ سعر كل حجم تضعه أنت يدويًا.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• الفواتير تحفظ نسخة من الأسعار وقت البيع، فتعديل الأسعار لا يغيّر الفواتير القديمة.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• ملفات PDF تُحفظ داخل مساحة التطبيق ويمكن مشاركتها من شاشة الفواتير.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
