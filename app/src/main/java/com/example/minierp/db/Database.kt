package com.example.minierp.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * كل البيانات محلية على الجهاز — بدون سيرفر أو إنترنت.
 * تم استخدام SQLite المدمج في أندرويد مباشرة (بدل Room) لتقليل حجم التطبيق.
 */
object DB {

    private var helper: DbHelper? = null

    fun init(context: Context) {
        if (helper == null) helper = DbHelper(context.applicationContext)
    }

    private fun db(): SQLiteDatabase =
        helper?.writableDatabase ?: error("يجب استدعاء DB.init(context) أولاً")

    private fun Cursor.s(col: String): String = getString(getColumnIndexOrThrow(col)) ?: ""
    private fun Cursor.d(col: String): Double = getDouble(getColumnIndexOrThrow(col))
    private fun Cursor.l(col: String): Long = getLong(getColumnIndexOrThrow(col))

    // ---------------------------------------------------------------- settings

    fun setting(key: String, default: String = ""): String =
        db().rawQuery("SELECT value FROM settings WHERE key=?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.s("value") else default
        }

    fun setSetting(key: String, value: String) {
        db().execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES(?,?)", arrayOf(key, value))
    }

    fun getSettings(): Settings = Settings(
        currency = setting("currency", "ج.م"),
        shopName = setting("shop_name"),
        shopPhone = setting("shop_phone"),
        shopAddress = setting("shop_address"),
        invoiceNote = setting("invoice_note")
    )

    fun saveSettings(s: Settings) {
        val d = db()
        d.beginTransaction()
        try {
            listOf(
                "currency" to s.currency,
                "shop_name" to s.shopName,
                "shop_phone" to s.shopPhone,
                "shop_address" to s.shopAddress,
                "invoice_note" to s.invoiceNote
            ).forEach { (k, v) -> setSetting(k, v) }
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    // -------------------------------------------------------------- ingredients

    fun ingredients(): List<Ingredient> {
        val out = mutableListOf<Ingredient>()
        db().rawQuery("SELECT * FROM ingredients ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) {
                out += Ingredient(c.l("id"), c.s("name"), c.d("price_per_gram"), c.s("note"))
            }
        }
        return out
    }

    fun addIngredient(name: String, pricePerGram: Double, note: String) {
        db().execSQL(
            "INSERT INTO ingredients(name,price_per_gram,note) VALUES(?,?,?)",
            arrayOf(name, pricePerGram, note)
        )
    }

    fun updateIngredient(id: Long, name: String, pricePerGram: Double, note: String) {
        db().execSQL(
            "UPDATE ingredients SET name=?,price_per_gram=?,note=? WHERE id=?",
            arrayOf(name, pricePerGram, note, id)
        )
    }

    fun deleteIngredient(id: Long) {
        val d = db()
        d.beginTransaction()
        try {
            d.execSQL("DELETE FROM formula_items WHERE ingredient_id=?", arrayOf(id))
            d.execSQL("DELETE FROM ingredients WHERE id=?", arrayOf(id))
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    // ----------------------------------------------------------------- formulas

    fun formulas(): List<Formula> {
        val out = mutableListOf<Formula>()
        db().rawQuery("SELECT * FROM formulas ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) {
                out += Formula(c.l("id"), c.s("name"), c.s("steps"), c.s("note"))
            }
        }
        return out
    }

    fun formulaItems(formulaId: Long): List<FormulaItem> {
        val out = mutableListOf<FormulaItem>()
        db().rawQuery("SELECT * FROM formula_items WHERE formula_id=?", arrayOf(formulaId)).use { c ->
            while (c.moveToNext()) {
                out += FormulaItem(
                    c.l("id"), c.l("formula_id"), c.l("ingredient_id"), c.d("grams")
                )
            }
        }
        return out
    }

    /** تكلفة المكونات للعرض فقط — لا تؤثر على سعر البيع إطلاقاً. */
    fun formulaCost(formulaId: Long): Double =
        db().rawQuery(
            """SELECT COALESCE(SUM(fi.grams * i.price_per_gram),0) AS cost
               FROM formula_items fi JOIN ingredients i ON i.id = fi.ingredient_id
               WHERE fi.formula_id=?""",
            arrayOf(formulaId)
        ).use { c -> if (c.moveToFirst()) c.d("cost") else 0.0 }

    fun saveFormula(
        id: Long,
        name: String,
        steps: String,
        note: String,
        items: List<Pair<Long, Double>>
    ): Long {
        val d = db()
        d.beginTransaction()
        try {
            val formulaId = if (id == 0L) {
                d.execSQL("INSERT INTO formulas(name,steps,note) VALUES(?,?,?)", arrayOf(name, steps, note))
                d.rawQuery("SELECT last_insert_rowid()", null).use { c ->
                    c.moveToFirst(); c.getLong(0)
                }
            } else {
                d.execSQL(
                    "UPDATE formulas SET name=?,steps=?,note=? WHERE id=?",
                    arrayOf(name, steps, note, id)
                )
                d.execSQL("DELETE FROM formula_items WHERE formula_id=?", arrayOf(id))
                id
            }
            items.forEach { (ingredientId, grams) ->
                d.execSQL(
                    "INSERT INTO formula_items(formula_id,ingredient_id,grams) VALUES(?,?,?)",
                    arrayOf(formulaId, ingredientId, grams)
                )
            }
            d.setTransactionSuccessful()
            return formulaId
        } finally {
            d.endTransaction()
        }
    }

    fun deleteFormula(id: Long) {
        val d = db()
        d.beginTransaction()
        try {
            d.execSQL("DELETE FROM formula_items WHERE formula_id=?", arrayOf(id))
            d.execSQL("UPDATE products SET formula_id=NULL WHERE formula_id=?", arrayOf(id))
            d.execSQL("DELETE FROM formulas WHERE id=?", arrayOf(id))
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    // ----------------------------------------------------------------- products

    fun products(): List<Product> {
        val out = mutableListOf<Product>()
        db().rawQuery("SELECT * FROM products ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) {
                val formulaId = if (c.isNull(c.getColumnIndexOrThrow("formula_id"))) null else c.l("formula_id")
                out += Product(c.l("id"), c.s("name"), formulaId, c.s("note"))
            }
        }
        return out
    }

    fun productSizes(productId: Long): List<ProductSize> {
        val out = mutableListOf<ProductSize>()
        db().rawQuery(
            "SELECT * FROM product_sizes WHERE product_id=? ORDER BY id",
            arrayOf(productId)
        ).use { c ->
            while (c.moveToNext()) {
                out += ProductSize(
                    c.l("id"), c.l("product_id"), c.s("label"), c.d("grams"), c.d("price")
                )
            }
        }
        return out
    }

    fun allSizes(): List<ProductSize> {
        val out = mutableListOf<ProductSize>()
        db().rawQuery("SELECT * FROM product_sizes ORDER BY product_id,id", null).use { c ->
            while (c.moveToNext()) {
                out += ProductSize(
                    c.l("id"), c.l("product_id"), c.s("label"), c.d("grams"), c.d("price")
                )
            }
        }
        return out
    }

    fun saveProduct(
        id: Long,
        name: String,
        formulaId: Long?,
        note: String,
        sizes: List<SizeInput>
    ): Long {
        val d = db()
        d.beginTransaction()
        try {
            val productId = if (id == 0L) {
                d.execSQL(
                    "INSERT INTO products(name,formula_id,note) VALUES(?,?,?)",
                    arrayOf(name, formulaId, note)
                )
                d.rawQuery("SELECT last_insert_rowid()", null).use { c ->
                    c.moveToFirst(); c.getLong(0)
                }
            } else {
                d.execSQL(
                    "UPDATE products SET name=?,formula_id=?,note=? WHERE id=?",
                    arrayOf(name, formulaId, note, id)
                )
                d.execSQL("DELETE FROM product_sizes WHERE product_id=?", arrayOf(id))
                id
            }
            sizes.filter { it.label.isNotBlank() }.forEach { s ->
                d.execSQL(
                    "INSERT INTO product_sizes(product_id,label,grams,price) VALUES(?,?,?,?)",
                    arrayOf(productId, s.label, s.grams, s.price)
                )
            }
            d.setTransactionSuccessful()
            return productId
        } finally {
            d.endTransaction()
        }
    }

    fun deleteProduct(id: Long) {
        val d = db()
        d.beginTransaction()
        try {
            d.execSQL("DELETE FROM product_sizes WHERE product_id=?", arrayOf(id))
            d.execSQL("DELETE FROM products WHERE id=?", arrayOf(id))
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    // ----------------------------------------------------------------- invoices

    fun nextInvoiceNo(): String {
        val n = setting("invoice_counter", "1").toIntOrNull()?.coerceAtLeast(1) ?: 1
        setSetting("invoice_counter", (n + 1).toString())
        return "INV-${n.toString().padStart(5, '0')}"
    }

    fun saveInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val d = db()
        d.beginTransaction()
        try {
            d.execSQL(
                """INSERT INTO invoices(invoice_no,customer_name,customer_phone,dateMillis,
                   subtotal,discount_type,discount_value,discount_amount,total,note)
                   VALUES(?,?,?,?,?,?,?,?,?,?)""",
                arrayOf(
                    invoice.invoiceNo, invoice.customerName, invoice.customerPhone,
                    invoice.dateMillis, invoice.subtotal, invoice.discountType,
                    invoice.discountValue, invoice.discountAmount, invoice.total, invoice.note
                )
            )
            val invoiceId = d.rawQuery("SELECT last_insert_rowid()", null).use { c ->
                c.moveToFirst(); c.getLong(0)
            }
            items.forEach { item ->
                d.execSQL(
                    """INSERT INTO invoice_items(invoice_id,product_name,size_label,
                       unit_price,qty,line_total,grams) VALUES(?,?,?,?,?,?,?)""",
                    arrayOf(
                        invoiceId, item.productName, item.sizeLabel, item.unitPrice,
                        item.qty, item.lineTotal, item.grams
                    )
                )
            }
            d.setTransactionSuccessful()
            return invoiceId
        } finally {
            d.endTransaction()
        }
    }

    fun invoices(): List<Invoice> {
        val out = mutableListOf<Invoice>()
        db().rawQuery("SELECT * FROM invoices ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) {
                out += Invoice(
                    id = c.l("id"),
                    invoiceNo = c.s("invoice_no"),
                    customerName = c.s("customer_name"),
                    customerPhone = c.s("customer_phone"),
                    dateMillis = c.l("dateMillis"),
                    subtotal = c.d("subtotal"),
                    discountType = c.s("discount_type"),
                    discountValue = c.d("discount_value"),
                    discountAmount = c.d("discount_amount"),
                    total = c.d("total"),
                    note = c.s("note")
                )
            }
        }
        return out
    }

    fun invoiceItems(invoiceId: Long): List<InvoiceItem> {
        val out = mutableListOf<InvoiceItem>()
        db().rawQuery("SELECT * FROM invoice_items WHERE invoice_id=?", arrayOf(invoiceId)).use { c ->
            while (c.moveToNext()) {
                out += InvoiceItem(
                    c.l("id"), c.l("invoice_id"), c.s("product_name"), c.s("size_label"),
                    c.d("unit_price"), c.d("qty"), c.d("line_total"), c.d("grams")
                )
            }
        }
        return out
    }

    fun deleteInvoice(id: Long) {
        val d = db()
        d.beginTransaction()
        try {
            d.execSQL("DELETE FROM invoice_items WHERE invoice_id=?", arrayOf(id))
            d.execSQL("DELETE FROM invoices WHERE id=?", arrayOf(id))
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    private class DbHelper(context: Context) :
        SQLiteOpenHelper(context, "minierp.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE settings (
                   key TEXT PRIMARY KEY, value TEXT NOT NULL)"""
            )
            db.execSQL(
                """CREATE TABLE ingredients (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   name TEXT NOT NULL,
                   price_per_gram REAL NOT NULL DEFAULT 0,
                   note TEXT NOT NULL DEFAULT '')"""
            )
            db.execSQL(
                """CREATE TABLE formulas (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   name TEXT NOT NULL,
                   steps TEXT NOT NULL DEFAULT '',
                   note TEXT NOT NULL DEFAULT '')"""
            )
            db.execSQL(
                """CREATE TABLE formula_items (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   formula_id INTEGER NOT NULL,
                   ingredient_id INTEGER NOT NULL,
                   grams REAL NOT NULL,
                   FOREIGN KEY(formula_id) REFERENCES formulas(id) ON DELETE CASCADE)"""
            )
            db.execSQL(
                """CREATE TABLE products (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   name TEXT NOT NULL,
                   formula_id INTEGER,
                   note TEXT NOT NULL DEFAULT '',
                   FOREIGN KEY(formula_id) REFERENCES formulas(id) ON DELETE SET NULL)"""
            )
            db.execSQL(
                """CREATE TABLE product_sizes (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   product_id INTEGER NOT NULL,
                   label TEXT NOT NULL,
                   grams REAL NOT NULL DEFAULT 0,
                   price REAL NOT NULL DEFAULT 0,
                   FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)"""
            )
            db.execSQL(
                """CREATE TABLE invoices (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   invoice_no TEXT NOT NULL UNIQUE,
                   customer_name TEXT NOT NULL DEFAULT '',
                   customer_phone TEXT NOT NULL DEFAULT '',
                   dateMillis INTEGER NOT NULL,
                   subtotal REAL NOT NULL DEFAULT 0,
                   discount_type TEXT NOT NULL DEFAULT 'none',
                   discount_value REAL NOT NULL DEFAULT 0,
                   discount_amount REAL NOT NULL DEFAULT 0,
                   total REAL NOT NULL DEFAULT 0,
                   note TEXT NOT NULL DEFAULT '')"""
            )
            db.execSQL(
                """CREATE TABLE invoice_items (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   invoice_id INTEGER NOT NULL,
                   product_name TEXT NOT NULL,
                   size_label TEXT NOT NULL,
                   unit_price REAL NOT NULL,
                   qty REAL NOT NULL,
                   line_total REAL NOT NULL,
                   grams REAL NOT NULL DEFAULT 0,
                   FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE)"""
            )
            db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES('currency','ج.م')")
            db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES('invoice_counter','1')")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // الإصدارات المستقبلية: أضف هنا هجرة البيانات مع الحفاظ على البيانات الحالية
        }
    }
}
