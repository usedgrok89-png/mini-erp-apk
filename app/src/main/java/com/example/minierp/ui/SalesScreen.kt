@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.minierp.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.minierp.db.DB
import com.example.minierp.db.Invoice
import com.example.minierp.db.InvoiceItem
import com.example.minierp.util.generateInvoicePdf

private data class CartLine(
    val productName: String,
    val sizeLabel: String,
    val unitPrice: Double,
    val qty: Double,
    val lineTotal: Double,
    val grams: Double
)

@Composable
fun SalesScreen(revision: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val products = remember(revision) { DB.products() }
    val allSizes = remember(revision) { DB.allSizes() }
    val settings = remember(revision) { DB.getSettings() }
    val currency = settings.currency
    val productNames = remember(products) { products.associate { it.id to it.name } }

    var productId by remember { mutableStateOf(0L) }
    var sizeId by remember { mutableStateOf(0L) }
    var qtyText by remember { mutableStateOf("1") }
    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf(0) } // 0 بلا، 1 نسبة، 2 مبلغ
    var discountValue by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<CartLine>() }

    val sizesOfProduct = allSizes.filter { it.productId == productId }
    val selectedSize = sizesOfProduct.firstOrNull { it.id == sizeId } ?: sizesOfProduct.firstOrNull()

    val subtotal = cart.sumOf { it.lineTotal }
    val dv = discountValue.toDoubleOrNull() ?: 0.0
    val discountAmount = when (discountType) {
        1 -> (subtotal * dv / 100.0).coerceAtMost(subtotal)
        2 -> dv.coerceAtMost(subtotal)
        else -> 0.0
    }
    val total = (subtotal - discountAmount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("بيع جديد", style = MaterialTheme.typography.titleMedium)
                Picker(
                    label = "المنتج",
                    selectedText = productNames[productId] ?: "",
                    options = products.map { it.id to it.name },
                    onSelect = { productId = it; sizeId = 0L }
                )
                Picker(
                    label = "الحجم",
                    selectedText = selectedSize?.label ?: "",
                    options = sizesOfProduct.map { it.id to "${it.label} — ${money(it.price, currency)}" },
                    onSelect = { sizeId = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("الكمية") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val q = qtyText.toDoubleOrNull() ?: 0.0
                            val s = selectedSize
                            when {
                                s == null -> toast(context, "اختر المنتج والحجم أولاً")
                                q <= 0 -> toast(context, "أدخل كمية صحيحة")
                                else -> {
                                    cart.add(
                                        CartLine(
                                            productName = productNames[productId].orEmpty(),
                                            sizeLabel = s.label,
                                            unitPrice = s.price,
                                            qty = q,
                                            lineTotal = s.price * q,
                                            grams = s.grams
                                        )
                                    )
                                    toast(context, "تمت الإضافة: ${s.label}")
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 4.dp)
                    ) { Text("إضافة") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customer, onValueChange = { customer = it },
                    label = { Text("اسم العميل (اختياري)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("هاتف العميل (اختياري)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المنتجات المضافة (${cart.size})", style = MaterialTheme.typography.titleSmall)
                if (cart.isEmpty()) {
                    Text(
                        "السلة فارغة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    cart.forEachIndexed { index, line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${line.productName} — ${line.sizeLabel}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${formatQty(line.qty)} × ${money(line.unitPrice, currency)} = ${money(line.lineTotal, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { cart.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "إزالة")
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الخصم", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = discountType == 0,
                        onClick = { discountType = 0 },
                        label = { Text("بدون") }
                    )
                    FilterChip(
                        selected = discountType == 1,
                        onClick = { discountType = 1 },
                        label = { Text("نسبة %") }
                    )
                    FilterChip(
                        selected = discountType == 2,
                        onClick = { discountType = 2 },
                        label = { Text("مبلغ") }
                    )
                }
                if (discountType != 0) {
                    OutlinedTextField(
                        value = discountValue,
                        onValueChange = { discountValue = it },
                        label = { Text(if (discountType == 1) "النسبة المئوية" else "قيمة الخصم ($currency)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider()
                SummaryRow("المجموع", money(subtotal, currency))
                if (discountAmount > 0) {
                    SummaryRow("الخصم", "- " + money(discountAmount, currency))
                }
                SummaryRow("الإجمالي", money(total, currency), bold = true)
            }
        }

        Button(
            onClick = {
                if (cart.isEmpty()) {
                    toast(context, "أضف منتجًا أولاً")
                } else {
                    val invoice = Invoice(
                        invoiceNo = DB.nextInvoiceNo(),
                        customerName = customer.trim(),
                        customerPhone = phone.trim(),
                        dateMillis = System.currentTimeMillis(),
                        subtotal = subtotal,
                        discountType = when (discountType) {
                            1 -> "percent"; 2 -> "amount"; else -> "none"
                        },
                        discountValue = dv,
                        discountAmount = discountAmount,
                        total = total
                    )
                    val items = cart.map {
                        InvoiceItem(
                            productName = it.productName,
                            sizeLabel = it.sizeLabel,
                            unitPrice = it.unitPrice,
                            qty = it.qty,
                            lineTotal = it.lineTotal,
                            grams = it.grams
                        )
                    }
                    val id = DB.saveInvoice(invoice, items)
                    val uri = generateInvoicePdf(context, invoice.copy(id = id), items, settings)
                    if (uri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
                    } else {
                        toast(context, "تم حفظ الفاتورة، لكن تعذر إنشاء PDF")
                    }
                    cart.clear()
                    customer = ""; phone = ""; discountValue = ""; discountType = 0
                    productId = 0L; sizeId = 0L; qtyText = "1"
                    onChanged()
                    toast(context, "تم حفظ الفاتورة ${invoice.invoiceNo}")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("حفظ الفاتورة وإنشاء PDF") }

        OutlinedButton(
            onClick = {
                cart.clear(); customer = ""; phone = ""; discountValue = ""
                discountType = 0; productId = 0L; sizeId = 0L; qtyText = "1"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("فاتورة جديدة") }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else String.format(java.util.Locale.US, "%.2f", q)
