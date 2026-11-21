package com.example.minierp.db

data class Ingredient(
    val id: Long = 0,
    val name: String,
    val pricePerGram: Double = 0.0,
    val note: String = ""
)

data class Formula(
    val id: Long = 0,
    val name: String,
    val steps: String = "",
    val note: String = ""
)

data class FormulaItem(
    val id: Long = 0,
    val formulaId: Long,
    val ingredientId: Long,
    val grams: Double
)

data class Product(
    val id: Long = 0,
    val name: String,
    val formulaId: Long? = null,
    val note: String = ""
)

data class SizeInput(
    val label: String,
    val grams: Double,
    val price: Double
)

data class ProductSize(
    val id: Long = 0,
    val productId: Long,
    val label: String,
    val grams: Double = 0.0,
    val price: Double = 0.0
)

data class Invoice(
    val id: Long = 0,
    val invoiceNo: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discountType: String = "none", // none | percent | amount
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val total: Double = 0.0,
    val note: String = ""
)

data class InvoiceItem(
    val id: Long = 0,
    val invoiceId: Long = 0,
    val productName: String,
    val sizeLabel: String,
    val unitPrice: Double,
    val qty: Double,
    val lineTotal: Double,
    val grams: Double = 0.0
)

data class Settings(
    val currency: String = "ج.م",
    val shopName: String = "",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val invoiceNote: String = ""
)
