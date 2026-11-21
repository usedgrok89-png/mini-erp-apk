@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.minierp.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minierp.db.DB
import com.example.minierp.db.Invoice
import com.example.minierp.util.generateInvoicePdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesScreen(revision: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val invoices = remember(revision) { DB.invoices() }
    val settings = remember(revision) { DB.getSettings() }
    var query by remember { mutableStateOf("") }
    val filtered = remember(invoices, query) {
        val q = query.trim()
        if (q.isBlank()) invoices else invoices.filter {
            it.invoiceNo.contains(q, ignoreCase = true) ||
                it.customerName.contains(q, ignoreCase = true) ||
                it.customerPhone.contains(q, ignoreCase = true)
        }
    }
    var selected by remember { mutableStateOf<Invoice?>(null) }
    var deleting by remember { mutableStateOf<Invoice?>(null) }

    LazyColumn(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ModernHeader(
                title = "الفواتير",
                subtitle = "محفوظة على جهازك — اضغط على فاتورة لعرضها أو مشاركتها PDF"
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("بحث في الفواتير (رقم / عميل / هاتف)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.id }) { inv ->
            Card(modifier = Modifier.fillMaxWidth(), onClick = { selected = inv }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(inv.invoiceNo, style = MaterialTheme.typography.titleMedium)
                        val date = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
                            .format(Date(inv.dateMillis))
                        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (inv.customerName.isNotBlank()) {
                            Text(inv.customerName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        money(inv.total, settings.currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyHint(
                    if (query.isBlank()) "لا توجد فواتير بعد — اعمل أول عملية بيع من شاشة المبيعات"
                    else "لا توجد نتائج مطابقة للبحث"
                )
            }
        }
    }

    selected?.let { inv ->
        val items = remember(inv.id) { DB.invoiceItems(inv.id) }
        Dialog(onDismissRequest = { selected = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("فاتورة ${inv.invoiceNo}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(Date(inv.dateMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (inv.customerName.isNotBlank()) Text("العميل: ${inv.customerName}")
                    if (inv.customerPhone.isNotBlank()) Text("الهاتف: ${inv.customerPhone}")
                    HorizontalDivider()

                    items.forEach { line ->
                        Column {
                            Text("${line.productName} — ${line.sizeLabel}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${formatNumber(line.qty)} × ${money(line.unitPrice, settings.currency)} = ${money(line.lineTotal, settings.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    SummaryLine("المجموع", money(inv.subtotal, settings.currency))
                    if (inv.discountAmount > 0) {
                        val label = if (inv.discountType == "percent") {
                            "الخصم (${formatNumber(inv.discountValue)}٪)"
                        } else "الخصم"
                        SummaryLine(label, "- " + money(inv.discountAmount, settings.currency))
                    }
                    SummaryLine("الإجمالي", money(inv.total, settings.currency), bold = true)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val uri = generateInvoicePdf(context, inv, items, settings)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
                                } else {
                                    Toast.makeText(context, "تعذر إنشاء PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("PDF / مشاركة") }
                        OutlinedButton(
                            onClick = { deleting = inv },
                            modifier = Modifier.weight(1f)
                        ) { Text("حذف") }
                    }
                    TextButton(onClick = { selected = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }

    deleting?.let { inv ->
        ConfirmDelete(
            title = "حذف الفاتورة",
            message = "سيتم حذف ${inv.invoiceNo} نهائيًا. هل أنت متأكد؟",
            onConfirm = { DB.deleteInvoice(inv.id); onChanged(); deleting = null; selected = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatNumber(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.2f", v)
