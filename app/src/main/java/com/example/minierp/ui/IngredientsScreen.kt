@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.minierp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.minierp.db.DB
import com.example.minierp.db.Ingredient

@Composable
fun IngredientsScreen(revision: Int, onChanged: () -> Unit) {
    val items = remember(revision) { DB.ingredients() }
    val currency = remember(revision) { DB.getSettings().currency }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Ingredient?>(null) }
    var deleting by remember { mutableStateOf<Ingredient?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة مكون")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "أسعار المكونات لكل جرام — للعلم فقط، لا تؤثر على سعر المنتج",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(items, key = { it.id }) { ing ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ing.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${money(ing.pricePerGram, currency)} / جرام",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (ing.note.isNotBlank()) {
                                Text(
                                    ing.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { editing = ing }) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                        }
                        IconButton(onClick = { deleting = ing }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
            if (items.isEmpty()) item { EmptyHint("لا توجد مكونات بعد — اضغط + للإضافة") }
        }
    }

    if (adding) {
        IngredientDialog(
            existing = null, currency = currency,
            onDismiss = { adding = false },
            onSave = { n, p, note -> DB.addIngredient(n, p, note); onChanged(); adding = false }
        )
    }
    editing?.let { current ->
        IngredientDialog(
            existing = current, currency = currency,
            onDismiss = { editing = null },
            onSave = { n, p, note -> DB.updateIngredient(current.id, n, p, note); onChanged(); editing = null }
        )
    }
    deleting?.let { current ->
        ConfirmDelete(
            title = "حذف المكون",
            message = "سيتم حذف «${current.name}» وحذفه من التركيبات التي يستخدمها. هل أنت متأكد؟",
            onConfirm = { DB.deleteIngredient(current.id); onChanged(); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun IngredientDialog(
    existing: Ingredient?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var price by remember { mutableStateOf(if (existing == null) "" else existing.pricePerGram.toString()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "مكون جديد" else "تعديل المكون") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المكون") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("السعر لكل جرام ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name.trim(), price.toDoubleOrNull() ?: 0.0, note.trim())
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
