@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.minierp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.minierp.db.DB
import com.example.minierp.db.Product
import com.example.minierp.db.SizeInput

@Composable
fun ProductsScreen(revision: Int, onChanged: () -> Unit) {
    val products = remember(revision) { DB.products() }
    val formulas = remember(revision) { DB.formulas() }
    val currency = remember(revision) { DB.getSettings().currency }
    val sizesByProduct = remember(revision, products) {
        products.associate { it.id to DB.productSizes(it.id) }
    }
    val formulaNames = remember(formulas) { formulas.associate { it.id to it.name } }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var deleting by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "منتج جديد")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "لكل حجم سعر مستقل يحدده المستخدم — السعر لا يُحسب من تكلفة المكونات",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(products, key = { it.id }) { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            val formulaName = p.formulaId?.let { formulaNames[it] }
                            if (!formulaName.isNullOrBlank()) {
                                Text(
                                    "التركيبة: $formulaName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val sizes = sizesByProduct[p.id].orEmpty()
                            if (sizes.isEmpty()) {
                                Text(
                                    "لا توجد أحجام — أضف حجمًا وسعره",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                sizes.forEach { s ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(s.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            money(s.price, currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { editing = p }) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                        }
                        IconButton(onClick = { deleting = p }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
            if (products.isEmpty()) item { EmptyHint("لا توجد منتجات بعد — اضغط + للإضافة") }
        }
    }

    if (adding || editing != null) {
        ProductDialog(
            existing = editing,
            formulas = formulas,
            currency = currency,
            onDismiss = { adding = false; editing = null },
            onSave = { name, formulaId, note, sizes ->
                DB.saveProduct(editing?.id ?: 0L, name, formulaId, note, sizes)
                onChanged(); adding = false; editing = null
            }
        )
    }
    deleting?.let { current ->
        ConfirmDelete(
            title = "حذف المنتج",
            message = "سيتم حذف «${current.name}» وكل أحجامه. الفواتير القديمة لن تتأثر.",
            onConfirm = { DB.deleteProduct(current.id); onChanged(); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

private data class SizeDraft(val label: String, val grams: String, val price: String)

@Composable
private fun ProductDialog(
    existing: Product?,
    formulas: List<com.example.minierp.db.Formula>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Long?, String, List<SizeInput>) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var formulaId by remember { mutableStateOf(existing?.formulaId) }
    val drafts = remember {
        mutableStateListOf<SizeDraft>().apply {
            if (existing != null) {
                DB.productSizes(existing.id).forEach {
                    add(SizeDraft(it.label, it.grams.toString(), it.price.toString()))
                }
            }
        }
    }
    var label by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val formulaOptions = listOf(0L to "بدون تركيبة") + formulas.map { it.id to it.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "منتج جديد" else "تعديل المنتج") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المنتج") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Picker(
                    label = "التركيبة",
                    selectedText = formulaOptions.firstOrNull { it.first == (formulaId ?: 0L) }?.second ?: "",
                    options = formulaOptions,
                    onSelect = { formulaId = if (it == 0L) null else it }
                )

                Text("الأحجام والأسعار", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("اسم الحجم (مثال: 30 جم)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grams, onValueChange = { grams = it },
                        label = { Text("الجرامات") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        label = { Text("السعر ($currency)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        if (label.isNotBlank()) {
                            drafts.add(SizeDraft(label.trim(), grams, price))
                            label = ""; grams = ""; price = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("إضافة الحجم") }

                drafts.forEachIndexed { index, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${s.label} — ${s.grams} جرام", style = MaterialTheme.typography.bodyMedium)
                            Text(money(s.price.toDoubleOrNull() ?: 0.0, currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { drafts.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "إزالة")
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("ملاحظات (اختياري)") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    name.trim(), formulaId, note.trim(),
                    drafts.map {
                        SizeInput(it.label, it.grams.toDoubleOrNull() ?: 0.0, it.price.toDoubleOrNull() ?: 0.0)
                    }
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
