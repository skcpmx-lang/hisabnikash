package com.hisabnikash.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.hisabnikash.app.data.db.BusinessEntity
import com.hisabnikash.app.data.db.BusinessSettingsEntity
import com.hisabnikash.app.data.db.CustomerEntity
import com.hisabnikash.app.data.db.InvoiceEntity
import com.hisabnikash.app.data.db.InvoiceItemEntity
import com.hisabnikash.app.domain.model.formatDate
import com.hisabnikash.app.domain.model.formatMoney
import java.io.File
import java.io.FileOutputStream

object DocumentPrinter {

    private const val PAGE_W = 595 // A4 at 72 dpi
    private const val PAGE_H = 842
    private const val MARGIN = 42

    /**
     * Renders an A4 invoice PDF. Pure platform APIs — no external PDF library.
     */
    fun buildInvoicePdf(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        business: BusinessEntity?,
        customer: CustomerEntity?,
        settings: BusinessSettingsEntity?
    ): PdfDocument {
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x0E, 0x6E, 0x5C)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        var y = MARGIN + 8

        // Header
        paint.textSize = 26f
        gold.textSize = 26f
        paint.setTypeface(Typeface.DEFAULT_BOLD)
        page.canvas.drawText(business?.name ?: "Business", MARGIN, y, paint)
        paint.setTypeface(Typeface.DEFAULT)
        paint.textSize = 12f
        y += 22
        page.canvas.drawText(business?.address ?: "", MARGIN, y, paint)
        y += 16
        page.canvas.drawText(business?.phone ?: "", MARGIN, y, paint)
        y += 16
        page.canvas.drawText(business?.email ?: "", MARGIN, y, paint)

        bold.textSize = 22f
        gold.textSize = 22f
        page.canvas.drawText("INVOICE", PAGE_W - MARGIN - 140f, MARGIN + 24, gold)
        bold.textSize = 13f
        page.canvas.drawText(invoice.invoiceNo, PAGE_W - MARGIN - 140f, MARGIN + 46, bold)
        paint.textSize = 12f
        page.canvas.drawText("Date: ${formatDate(invoice.dateAt)}", PAGE_W - MARGIN - 160f, MARGIN + 66, paint)
        if (invoice.dueDateAt != null) {
            page.canvas.drawText("Due: ${formatDate(invoice.dueDateAt)}", PAGE_W - MARGIN - 160f, MARGIN + 84, paint)
        }

        y = MARGIN + 150
        // Bill to
        bold.textSize = 13f
        page.canvas.drawText("Bill to", MARGIN, y, bold)
        paint.textSize = 12f
        y += 20
        page.canvas.drawText(customer?.name ?: "Walk-in customer", MARGIN, y, paint)
        customer?.address?.let {
            y += 16
            page.canvas.drawText(it, MARGIN, y, paint)
        }
        customer?.phone?.let {
            y += 16
            page.canvas.drawText(it, MARGIN, y, paint)
        }

        y += 40
        // Table header
        val col1 = MARGIN
        val col2 = MARGIN + 300
        val col3 = MARGIN + 400
        val col4 = MARGIN + 460
        gold.textSize = 11f
        page.canvas.drawText("ITEM", col1, y, bold)
        page.canvas.drawText("QTY", col2, y, bold)
        page.canvas.drawText("PRICE", col3, y, bold)
        page.canvas.drawText("TOTAL", col4, y, bold)
        y += 8
        page.canvas.drawLine(MARGIN.toFloat(), y, PAGE_W - MARGIN.toFloat(), y, paint)
        y += 22

        paint.textSize = 12f
        for (item in items) {
            page.canvas.drawText(item.name.take(34), col1, y, paint)
            page.canvas.drawText("${item.qty}", col2, y, paint)
            page.canvas.drawText(formatMoney(item.unitPriceMinor), col3, y, paint)
            page.canvas.drawText(formatMoney(item.lineTotalMinor), col4, y, paint)
            y += 18
        }

        y += 20
        paint.textSize = 12f
        page.canvas.drawText("Subtotal", col3, y, paint)
        page.canvas.drawText(formatMoney(invoice.subtotalMinor), col4, y, bold)
        y += 18
        if (invoice.discountMinor > 0) {
            page.canvas.drawText("Discount", col3, y, paint)
            page.canvas.drawText("-" + formatMoney(invoice.discountMinor), col4, y, bold)
            y += 18
        }
        if (invoice.deliveryMinor > 0) {
            page.canvas.drawText("Delivery", col3, y, paint)
            page.canvas.drawText(formatMoney(invoice.deliveryMinor), col4, y, bold)
            y += 18
        }
        if (invoice.taxMinor > 0) {
            page.canvas.drawText("Tax", col3, y, paint)
            page.canvas.drawText(formatMoney(invoice.taxMinor), col4, y, bold)
            y += 18
        }
        y += 4
        bold.textSize = 14f
        page.canvas.drawText("TOTAL", col3, y, bold)
        page.canvas.drawText(formatMoney(invoice.totalMinor), col4, y, bold)
        y += 22
        paint.textSize = 12f
        if (invoice.advanceMinor > 0) {
            page.canvas.drawText("Advance paid", col3, y, paint)
            page.canvas.drawText(formatMoney(invoice.advanceMinor), col4, y, bold)
            y += 18
        }
        page.canvas.drawText("Balance due", col3, y, bold)
        page.canvas.drawText(formatMoney((invoice.totalMinor - invoice.advanceMinor - invoice.paidMinor).coerceAtLeast(0)), col4, y, bold)

        y += 44
        paint.textSize = 11f
        if (invoice.terms.isNotBlank()) {
            page.canvas.drawText("Terms", MARGIN, y, bold)
            y += 16
            var lineY = y
            invoice.terms.split("\n").take(3).forEach { line ->
                page.canvas.drawText(line.take(84), MARGIN, lineY, paint)
                lineY += 14
            }
            y = lineY + 20
        }
        if (invoice.footer.isNotBlank()) {
            page.canvas.drawText(invoice.footer.take(84), MARGIN, y, paint)
            y += 16
        }
        paint.textSize = 9f
        page.canvas.drawText("Generated by HisabNikash", MARGIN, PAGE_H - MARGIN, paint)

        // Multi-page safety: if content ran off the page, start a second page.
        if (y > PAGE_H - MARGIN) {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 2).create())
            paint.textSize = 11f
            page.canvas.drawText("Continued", MARGIN, MARGIN + 10, paint)
            var yy = MARGIN + 30
            for (item in items) {
                page.canvas.drawText("${item.name.take(40)}   ${item.qty} x ${formatMoney(item.unitPriceMinor)}", MARGIN, yy, paint)
                yy += 14
            }
        }
        doc.finishPage(page)
        return doc
    }

    fun writePdf(context: Context, doc: PdfDocument, baseName: String): File {
        val dir = File(context.filesDir, "documents").apply { mkdirs() }
        val file = File(dir, "$baseName.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share invoice"))
    }

    fun printPdf(context: Context, label: String, file: File) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: PrintDocumentAdapter.LayoutResultCallback,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder(label)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build(),
                    true
                )
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: PrintDocumentAdapter.WriteResultCallback
            ) {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                        file.inputStream().use { it.copyTo(output) }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        manager.print(label, adapter, PrintAttributes.Builder().build())
    }
}
