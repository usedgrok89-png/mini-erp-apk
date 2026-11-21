@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.minierp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.minierp.db.DB
import com.example.minierp.ui.FormulasScreen
import com.example.minierp.ui.IngredientsScreen
import com.example.minierp.ui.InvoicesScreen
import com.example.minierp.ui.MiniERPTheme
import com.example.minierp.ui.ProductsScreen
import com.example.minierp.ui.SalesScreen
import com.example.minierp.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DB.init(this)
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val tabs = listOf("المكونات", "التركيبات", "المنتجات", "المبيعات", "الفواتير", "الإعدادات")
    var selected by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    val bump: () -> Unit = { revision++ }
    val theme = remember(revision) { DB.getSettings().theme }

    MiniERPTheme(theme = theme) {
        // نفرض اتجاه عربي RTL حتى لو لغة الجهاز إنجليزية
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    topBar = {
                        ScrollableTabRow(
                            selectedTabIndex = selected,
                            edgePadding = 8.dp,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selected == index,
                                    onClick = { selected = index },
                                    text = { Text(title) },
                                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        when (selected) {
                            0 -> IngredientsScreen(revision, bump)
                            1 -> FormulasScreen(revision, bump)
                            2 -> ProductsScreen(revision, bump)
                            3 -> SalesScreen(revision, bump)
                            4 -> InvoicesScreen(revision, bump)
                            5 -> SettingsScreen(revision, bump)
                        }
                    }
                }
            }
        }
    }
}
