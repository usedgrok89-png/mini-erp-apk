package com.example.minierp.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.minierp.R
import com.example.minierp.db.Invoice
import com.example.minierp.db.InvoiceItem
import com.example.minierp.db.Settings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * توليد PDF محليًا باستخدام PdfDocument المدمج في أندرويد — بدون أي مكتبة خارجية.
 * النص العربي يظهر مشكّلًا بشكل صحيح لأن Canvas في أندرويد يدعم التشكيل العربي تلقائيًا.
 */
fun generateInvoicePdf(
    context: Context,
    invoice: Invoice,
    items: List<InvoiceItem>,
    settings: Settings
): Uri? = try {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 نقطة

    val regular = ResourcesCompat.getFont(context, R.font.tajawal_regular)
    val bold = ResourcesCompat.getFont(context, R.font.tajawal_bold)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 90, 77); textSize = 20f; typeface = bold; textAlign = Paint.Align.CENTER
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(90, 105, 98); textSize = 11f; typeface = regular; textAlign = Paint.Align.CENTER
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 90, 77); textSize = 11f; typeface = bold; textAlign = Paint.Align.RIGHT
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 41, 59); textSize = 10.5f; typeface = regular; textAlign = Paint.Align.RIGHT
    }
    val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 90, 77); textSize = 13f; typeface = bold; textAlign = Paint.Align.RIGHT
    }
    val linePaint = Paint().apply {
        color = Color.rgb(214, 222, 217); strokeWidth = 1f
    }

    val currency = settings.currency
    val dateText = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(Date(invoice.dateMillis))
    val shopName = settings.shopName.ifBlank { "مدير المنتجات" }

    var page = doc.startPage(pageInfo)
    var canvas = page.canvas
    var y = 55f
    var pageNo = 1

    fun rule(c: Canvas, yy: Float) {
        c.drawLine(40f, yy, 555f, yy, linePaint)
    }

    fun drawHeader(c: Canvas) {
        c.drawText(shopName, 297.5f, y, titlePaint); y += 24f
        c.drawText("فاتورة مبيعات", 297.5f, y, subtitlePaint); y += 22f
        rule(c, y); y += 22f

        c.drawText("رقم الفاتورة:  ${invoice.invoiceNo}", 555f, y, textPaint)
        c.drawText("التاريخ:  $dateText", 40f, y, Paint(textPaint).apply { textAlign = Paint.Align.LEFT }); y += 18f
        c.drawText("العميل:  ${invoice.customerName.ifBlank { "-" }}", 555f, y, textPaint)
        if (settings.shopPhone.isNotBlank()) {
            c.drawText(settings.shopPhone, 40f, y, Paint(textPaint).apply { textAlign = Paint.Align.LEFT })
        }
        y += 18f
        if (invoice.customerPhone.isNotBlank()) {
            c.drawText("الهاتف:  ${invoice.customerPhone}", 555f, y, textPaint); y += 18f
        }
        if (settings.shopAddress.isNotBlank()) {
            c.drawText(settings.shopAddress, 40f, y, Paint(textPaint).apply { textAlign = Paint.Align.LEFT }); y += 18f
        }
        rule(c, y); y += 24f
    }

    fun drawTableHead(c: Canvas) {
        c.drawText("الصنف", 555f, y, labelPaint)
        c.drawText("الكمية", 330f, y, labelPaint)
        c.drawText("السعر", 245f, y, labelPaint)
        c.drawText("المجموع", 150f, y, labelPaint)
        y += 10f
        rule(c, y); y += 20f
    }

    fun newPage() {
        doc.finishPage(page)
        pageNo++
        page = doc.startPage(pageInfo)
        canvas = page.canvas
        y = 55f
        drawHeader(canvas)
        drawTableHead(canvas)
    }

    drawHeader(canvas)
    drawTableHead(canvas)

    fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val count = paint.breakText(text, true, maxWidth, null)
        return text.take(count) + "…"
    }

    items.forEach { item ->
        if (y > 730f) newPage()
        val name = fit("${item.productName} (${item.sizeLabel})", textPaint, 210f)
        canvas.drawText(name, 555f, y, textPaint)
        canvas.drawText(formatNum(item.qty), 330f, y, textPaint)
        canvas.drawText(formatNum(item.unitPrice), 245f, y, textPaint)
        canvas.drawText("${formatNum(item.lineTotal)} $currency", 150f, y, textPaint)
        y += 24f
    }

    y += 12f
    rule(canvas, y); y += 24f
    canvas.drawText("المجموع:  ${formatNum(invoice.subtotal)} $currency", 320f, y, textPaint); y += 24f
    if (invoice.discountAmount > 0) {
        val label = if (invoice.discountType == "percent") {
            "الخصم (${formatNum(invoice.discountValue)}٪):"
        } else {
            "الخصم:"
        }
        canvas.drawText("$label  -${formatNum(invoice.discountAmount)} $currency", 320f, y, textPaint); y += 24f
    }
    canvas.drawText("الإجمالي:  ${formatNum(invoice.total)} $currency", 320f, y, totalPaint); y += 30f

    if (settings.invoiceNote.isNotBlank()) {
        rule(canvas, y); y += 20f
        canvas.drawText(settings.invoiceNote, 555f, y, textPaint); y += 24f
    }

    canvas.drawText("شكرًا لتعاونكم", 297.5f, 800f, subtitlePaint)
    canvas.drawText("صفحة $pageNo", 297.5f, 820f, subtitlePaint)

    doc.finishPage(page)
    val dir = File(context.filesDir, "documents/invoices").apply { mkdirs() }
    val file = File(dir, "${invoice.invoiceNo}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()

    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (e: Exception) {
    null
}

private fun formatNum(value: Double): String = String.format(Locale.US, "%.2f", value)
