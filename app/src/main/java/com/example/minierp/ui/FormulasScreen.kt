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
import com.example.minierp.db.Formula

@Composable
fun FormulasScreen(revision: Int, onChanged: () -> Unit) {
    val formulas = remember(revision) { DB.formulas() }
    val ingredients = remember(revision) { DB.ingredients() }
    val currency = remember(revision) { DB.getSettings().currency }
    val costs = remember(revision, formulas) { formulas.associate { it.id to DB.formulaCost(it.id) } }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Formula?>(null) }
    var deleting by remember { mutableStateOf<Formula?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "تركيبة جديدة")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "التركيبة = المكونات بجراماتها + خطوات التصنيع",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(formulas, key = { it.id }) { f ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "تكلفة المكونات (للعلم فقط): ${money(costs[f.id] ?: 0.0, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (f.steps.isNotBlank()) {
                                Text(
                                    f.steps,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                        IconButton(onClick = { editing = f }) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                        }
                        IconButton(onClick = { deleting = f }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
            if (formulas.isEmpty()) item { EmptyHint("لا توجد تركيبات بعد — اضغط + للإضافة") }
        }
    }

    if (adding || editing != null) {
        FormulaDialog(
            existing = editing,
            ingredients = ingredients,
            currency = currency,
            onDismiss = { adding = false; editing = null },
            onSave = { name, steps, note, items ->
                DB.saveFormula(editing?.id ?: 0L, name, steps, note, items)
                onChanged(); adding = false; editing = null
            }
        )
    }
    deleting?.let { current ->
        ConfirmDelete(
            title = "حذف التركيبة",
            message = "سيتم حذف «${current.name}» وفصلها عن المنتجات المرتبطة بها. هل أنت متأكد؟",
            onConfirm = { DB.deleteFormula(current.id); onChanged(); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun FormulaDialog(
    existing: Formula?,
    ingredients: List<Ingredient>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<Pair<Long, Double>>) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var steps by remember { mutableStateOf(existing?.steps ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    val drafts = remember {
        mutableStateListOf<Pair<Long, String>>().apply {
            if (existing != null) {
                DB.formulaItems(existing.id).forEach { add(it.ingredientId to it.grams.toString()) }
            }
        }
    }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var grams by remember { mutableStateOf("") }

    val ingMap = remember(ingredients) { ingredients.associateBy { it.id } }
    val estimated = drafts.sumOf { (id, g) ->
        (ingMap[id]?.pricePerGram ?: 0.0) * (g.toDoubleOrNull() ?: 0.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "تركيبة جديدة" else "تعديل التركيبة") },
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
                    label = { Text("اسم التركيبة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Picker(
                    label = "المكون",
                    selectedText = ingMap[selectedId]?.name ?: "",
                    options = ingredients.map { it.id to it.name },
                    onSelect = { selectedId = it }
                )
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("الكمية بالجرام") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val id = selectedId
                        val g = grams.toDoubleOrNull() ?: 0.0
                        if (id != null && g > 0) {
                            drafts.add(id to g.toString())
                            grams = ""
                            selectedId = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("إضافة المكون للتركيبة") }

                drafts.forEachIndexed { index, (id, g) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${ingMap[id]?.name ?: "مكون محذوف"}  —  $g جرام",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { drafts.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "إزالة")
                        }
                    }
                }
                Text(
                    "تكلفة المكونات (للعلم فقط): ${money(estimated, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text("خطوات التصنيع") },
                    minLines = 3,
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
                onSave(
                    name.trim(), steps.trim(), note.trim(),
                    drafts.map { it.first to (it.second.toDoubleOrNull() ?: 0.0) }
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
