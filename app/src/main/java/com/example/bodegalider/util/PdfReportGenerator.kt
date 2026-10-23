package com.example.bodegalider.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.bodegalider.model.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generatePdf(context: Context, products: List<Product>, storeCode: String): File? {
        try {
            val pdfDocument = PdfDocument()
            val titlePaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint().apply {
                color = Color.rgb(100, 100, 100)
                textSize = 10f
            }
            val brandPaint = Paint().apply {
                color = Color.rgb(198, 40, 40)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = Color.rgb(50, 50, 50)
                textSize = 11f
            }
            val boldTextPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }

            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Header
            canvas.drawText("REPORTE DE QUIEBRES - BODEGA LIDER", 40f, 50f, titlePaint)
            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Código de Local: $storeCode | Fecha: $currentDate | Total críticos: ${products.size}", 40f, 70f, subtitlePaint)

            canvas.drawLine(40f, 85f, (pageWidth - 40).toFloat(), 85f, linePaint)

            var yPos = 110f
            val itemHeight = 90f

            products.forEach { product ->
                if (yPos + itemHeight > pageHeight - 50) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                val cardPaint = Paint().apply {
                    color = Color.rgb(255, 245, 245)
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                    color = Color.rgb(239, 154, 154)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                val rect = RectF(40f, yPos, (pageWidth - 40).toFloat(), yPos + itemHeight - 10)
                canvas.drawRoundRect(rect, 8f, 8f, cardPaint)
                canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

                // Product Image Thumbnail
                val imgX = 55f
                val imgY = yPos + 10f
                val imgSize = 60f
                val imgRect = RectF(imgX, imgY, imgX + imgSize, imgY + imgSize)

                if (product.imageResId != 0) {
                    try {
                        val bitmap = BitmapFactory.decodeResource(context.resources, product.imageResId)
                        if (bitmap != null) {
                            canvas.drawBitmap(bitmap, null, imgRect, null)
                        }
                    } catch (exc: Exception) {
                        canvas.drawRect(imgRect, Paint().apply { color = Color.LTGRAY })
                    }
                } else {
                    canvas.drawRect(imgRect, Paint().apply { color = Color.LTGRAY })
                }

                val textStartX = imgX + imgSize + 15f
                canvas.drawText(product.brand.uppercase(), textStartX, yPos + 22f, brandPaint)
                canvas.drawText(product.name, textStartX, yPos + 38f, boldTextPaint)
                canvas.drawText("Código: ${product.barcode}  |  Ubicación: ${product.locationText}", textStartX, yPos + 54f, textPaint)
                canvas.drawText("En Góndola: ${product.stockOnShelf} un.   |   Por Rellenar: ${product.quantityNeeded} un.", textStartX, yPos + 70f, boldTextPaint)

                yPos += itemHeight
            }

            pdfDocument.finishPage(page)

            val pdfFile = File(context.cacheDir, "Reporte_Quiebres_Local_${storeCode}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            return pdfFile
        } catch (exc: Exception) {
            exc.printStackTrace()
            return null
        }
    }
}
