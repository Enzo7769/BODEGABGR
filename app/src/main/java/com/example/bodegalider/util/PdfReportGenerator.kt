package com.example.bodegalider.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.example.bodegalider.model.InventoryStats
import com.example.bodegalider.model.Product
import com.example.bodegalider.model.StockStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 en puntos
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val HEADER_HEIGHT = 96f
    private const val ROW_HEIGHT = 46f
    private const val FOOTER_HEIGHT = 36f

    private val BRAND_BLUE = Color.rgb(0, 113, 206)
    private val BRAND_YELLOW = Color.rgb(255, 194, 32)
    private val TEXT_DARK = Color.rgb(26, 28, 32)
    private val TEXT_MUTED = Color.rgb(100, 104, 112)
    private val DIVIDER = Color.rgb(222, 226, 232)
    private val ZEBRA = Color.rgb(246, 248, 252)

    // Columnas: producto | ubicación | góndola | reponer | estado
    private val COL_PRODUCT = MARGIN + 8f
    private val COL_LOCATION = MARGIN + 220f
    private val COL_STOCK = MARGIN + 350f
    private val COL_NEEDED = MARGIN + 405f
    private val COL_STATUS = MARGIN + 450f

    fun generatePdf(context: Context, products: List<Product>, storeName: String, generatedBy: String? = null): File? {
        val document = PdfDocument()
        return try {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val stats = InventoryStats.from(products)

            val firstPageRows = ((PAGE_HEIGHT - HEADER_HEIGHT - 110f - FOOTER_HEIGHT - MARGIN) / ROW_HEIGHT).toInt()
            val otherPageRows = ((PAGE_HEIGHT - MARGIN - 30f - FOOTER_HEIGHT - MARGIN) / ROW_HEIGHT).toInt()
            val totalPages = if (products.size <= firstPageRows) 1
            else 1 + (products.size - firstPageRows + otherPageRows - 1) / otherPageRows

            var index = 0
            for (pageNumber in 1..totalPages) {
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                val canvas = page.canvas
                var y: Float

                if (pageNumber == 1) {
                    drawHeader(canvas, storeName, date, generatedBy)
                    y = HEADER_HEIGHT + 24f
                    drawSummary(canvas, y, stats, products.size)
                    y += 70f
                } else {
                    y = MARGIN
                }

                y = drawTableHeader(canvas, y)
                val rows = if (pageNumber == 1) firstPageRows else otherPageRows
                repeat(rows) {
                    if (index < products.size) {
                        drawRow(canvas, y, products[index], zebra = index % 2 == 1)
                        y += ROW_HEIGHT
                        index++
                    }
                }

                drawFooter(canvas, pageNumber, totalPages, date)
                document.finishPage(page)
            }

            val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // Solo se conserva el último reporte
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileStore = storeName.normalizeForSearch().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "sala" }
            val file = File(dir, "Reporte_Quiebres_${fileStore}_$stamp.pdf")
            file.outputStream().use { document.writeTo(it) }
            file
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error al generar el PDF", e)
            null
        } finally {
            document.close()
        }
    }

    private fun drawHeader(canvas: Canvas, storeName: String, date: String, generatedBy: String?) {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), HEADER_HEIGHT, fill(BRAND_BLUE))
        canvas.drawRect(0f, HEADER_HEIGHT - 5f, PAGE_WIDTH.toFloat(), HEADER_HEIGHT, fill(BRAND_YELLOW))

        canvas.drawText("Reporte de quiebres y stock bajo", MARGIN, 44f, text(Color.WHITE, 20f, bold = true))
        val subtitle = buildString {
            append("$storeName · $date")
            if (!generatedBy.isNullOrBlank()) append(" · Generado por $generatedBy")
        }
        canvas.drawText(subtitle, MARGIN, 66f, text(Color.argb(220, 255, 255, 255), 10f))
    }

    private fun drawSummary(canvas: Canvas, top: Float, stats: InventoryStats, listed: Int) {
        val items = listOf(
            "Productos en reporte" to "$listed",
            "Sin stock" to "${stats.outOfStock}",
            "Stock bajo" to "${stats.lowStock}",
            "Unidades a reponer" to "${stats.unitsToRestock}"
        )
        val gap = 10f
        val width = (PAGE_WIDTH - 2 * MARGIN - gap * (items.size - 1)) / items.size
        items.forEachIndexed { i, (label, value) ->
            val left = MARGIN + i * (width + gap)
            val rect = RectF(left, top, left + width, top + 54f)
            canvas.drawRoundRect(rect, 8f, 8f, fill(ZEBRA))
            canvas.drawRoundRect(rect, 8f, 8f, stroke(DIVIDER))
            val valueColor = when (i) {
                1 -> Color.rgb(186, 26, 26)
                2 -> Color.rgb(178, 94, 0)
                else -> BRAND_BLUE
            }
            canvas.drawText(value, left + 12f, top + 26f, text(valueColor, 18f, bold = true))
            canvas.drawText(label, left + 12f, top + 44f, text(TEXT_MUTED, 9f))
        }
    }

    private fun drawTableHeader(canvas: Canvas, top: Float): Float {
        val paint = text(TEXT_MUTED, 9f, bold = true)
        val baseline = top + 14f
        canvas.drawText("PRODUCTO", COL_PRODUCT, baseline, paint)
        canvas.drawText("UBICACIÓN", COL_LOCATION, baseline, paint)
        canvas.drawText("GÓNDOLA", COL_STOCK, baseline, paint)
        canvas.drawText("REPONER", COL_NEEDED, baseline, paint)
        canvas.drawText("ESTADO", COL_STATUS, baseline, paint)
        canvas.drawLine(MARGIN, top + 22f, PAGE_WIDTH - MARGIN, top + 22f, stroke(TEXT_MUTED))
        return top + 26f
    }

    private fun drawRow(canvas: Canvas, top: Float, product: Product, zebra: Boolean) {
        if (zebra) canvas.drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + ROW_HEIGHT, fill(ZEBRA))

        val brandPaint = text(BRAND_BLUE, 8f, bold = true)
        val namePaint = text(TEXT_DARK, 10.5f, bold = true)
        val bodyPaint = text(TEXT_DARK, 10f)
        val mutedPaint = text(TEXT_MUTED, 8.5f)

        canvas.drawText(ellipsize(product.brand.uppercase(), brandPaint, COL_LOCATION - COL_PRODUCT - 10f), COL_PRODUCT, top + 15f, brandPaint)
        canvas.drawText(ellipsize(product.name, namePaint, COL_LOCATION - COL_PRODUCT - 10f), COL_PRODUCT, top + 28f, namePaint)
        canvas.drawText(ellipsize("Cód. ${product.barcode}", mutedPaint, COL_LOCATION - COL_PRODUCT - 10f), COL_PRODUCT, top + 39f, mutedPaint)

        canvas.drawText(ellipsize(product.locationText, bodyPaint, COL_STOCK - COL_LOCATION - 8f), COL_LOCATION, top + 27f, bodyPaint)
        canvas.drawText("${product.stockOnShelf}", COL_STOCK + 10f, top + 27f, bodyPaint)
        canvas.drawText("${product.quantityNeeded}", COL_NEEDED + 10f, top + 27f, text(TEXT_DARK, 10f, bold = true))

        val (bg, fg) = when (product.stockStatus) {
            StockStatus.OUT_OF_STOCK -> Color.rgb(255, 218, 214) to Color.rgb(147, 0, 10)
            StockStatus.LOW -> Color.rgb(255, 227, 194) to Color.rgb(110, 58, 0)
            StockStatus.OK -> Color.rgb(212, 245, 220) to Color.rgb(0, 82, 37)
        }
        val badgePaint = text(fg, 8.5f, bold = true)
        val label = product.stockStatus.label
        val badgeWidth = badgePaint.measureText(label) + 14f
        val badge = RectF(COL_STATUS, top + 15f, COL_STATUS + badgeWidth, top + 31f)
        canvas.drawRoundRect(badge, 8f, 8f, fill(bg))
        canvas.drawText(label, COL_STATUS + 7f, top + 26.5f, badgePaint)

        canvas.drawLine(MARGIN, top + ROW_HEIGHT, PAGE_WIDTH - MARGIN, top + ROW_HEIGHT, stroke(DIVIDER))
    }

    private fun drawFooter(canvas: Canvas, page: Int, total: Int, date: String) {
        val y = PAGE_HEIGHT - MARGIN + 10f
        canvas.drawLine(MARGIN, y - 16f, PAGE_WIDTH - MARGIN, y - 16f, stroke(DIVIDER))
        val paint = text(TEXT_MUTED, 8.5f)
        canvas.drawText("Bodega Líder · Generado el $date", MARGIN, y, paint)
        val pageText = "Página $page de $total"
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - paint.measureText(pageText), y, paint)
    }

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(value) <= maxWidth) return value
        var end = value.length
        while (end > 0 && paint.measureText(value, 0, end) + paint.measureText("…") > maxWidth) end--
        return value.substring(0, end).trimEnd() + "…"
    }

    private fun text(color: Int, size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }
}
