package com.example.invoice_management.service;

import com.example.invoice_management.entity.ClientCompany;
import com.example.invoice_management.entity.Invoice;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PDFService {

    // ===== COLOR THEME =====
    private static final Color PRIMARY_COLOR = new DeviceRgb(25, 118, 210); // Blue
    private static final Color SECONDARY_COLOR = new DeviceRgb(56, 142, 60); // Green
    private static final Color LIGHT_GRAY = new DeviceRgb(245, 245, 245);
    private static final Color DARK_GRAY = new DeviceRgb(66, 66, 66);
    private static final Color WHITE = new DeviceRgb(255, 255, 255);

    public String generateInvoicePDF(Invoice invoice) {

        try {
            String fileName = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
            String filePath = Paths.get("invoices", fileName).toString();
            // Ensure invoices directory exists (ignore result intentionally but check for potential failure in future)
            java.io.File invoicesDir = new java.io.File("invoices");
            if (!invoicesDir.exists()) {
                invoicesDir.mkdirs();
            }

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(20, 20, 20, 20);

            // =====================================================
            // COMPANY HEADER (CENTERED)
            // =====================================================
            document.add(new Paragraph(invoice.getOurCompany().getCompanyName())
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(invoice.getOurCompany().getAddress())
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("GSTIN: " + invoice.getOurCompany().getGstNumber())
                    .setFontSize(9)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(
                    "Email: " + invoice.getOurCompany().getEmail()
                            + " | Phone: " + invoice.getOurCompany().getPhone())
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // =====================================================
            // TAX INVOICE TITLE
            // =====================================================
            document.add(new Paragraph("TAX INVOICE")
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(DARK_GRAY));

            document.add(new Paragraph("\n"));

            // =====================================================
            // INVOICE META (RIGHT)
            // =====================================================
            Table metaTable = new Table(1);
            metaTable.setWidth(UnitValue.createPercentValue(100));

            Cell metaCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);

            metaCell.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
            metaCell.add(new Paragraph("Invoice Date: " +
                    invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))));
            metaCell.add(new Paragraph("PO Reference: " +
                    invoice.getPurchaseOrder().getPoNumber()));

            metaTable.addCell(metaCell);
            document.add(metaTable);
            document.add(new Paragraph("\n"));

            // =====================================================
            // BILL FROM / BILL TO
            // =====================================================
            Table billTable = new Table(2);
            billTable.setWidth(UnitValue.createPercentValue(100));

            // Bill From
            Cell fromCell = new Cell()
                    .setBackgroundColor(LIGHT_GRAY)
                    .setPadding(10);

            fromCell.add(new Paragraph("Bill From").setBold());
            fromCell.add(new Paragraph(invoice.getOurCompany().getCompanyName()));
            fromCell.add(new Paragraph(invoice.getOurCompany().getEmail()));
            fromCell.add(new Paragraph(invoice.getOurCompany().getPhone()));
            fromCell.add(new Paragraph(invoice.getOurCompany().getAddress()));

            billTable.addCell(fromCell);

            // Bill To
            ClientCompany client = invoice.getPurchaseOrder().getClientCompany();
            Cell toCell = new Cell()
                    .setBackgroundColor(LIGHT_GRAY)
                    .setPadding(10);

            toCell.add(new Paragraph("Bill To").setBold());
            toCell.add(new Paragraph(client.getCompanyName()));
            toCell.add(new Paragraph(client.getEmail()));
            toCell.add(new Paragraph(client.getPhone()));
            toCell.add(new Paragraph(client.getAddress()));

            billTable.addCell(toCell);
            document.add(billTable);

            document.add(new Paragraph("\n"));

            // =====================================================
            // SERVICE DETAILS TABLE
            // =====================================================
            Table serviceTable = new Table(new float[]{1, 4, 2, 2, 2});
            serviceTable.setWidth(UnitValue.createPercentValue(100));

            serviceTable.addHeaderCell(header("S.No"));
            serviceTable.addHeaderCell(header("Service Description"));
            serviceTable.addHeaderCell(header("Duration"));
            serviceTable.addHeaderCell(header("Start Date"));
            serviceTable.addHeaderCell(header("Amount"));

            serviceTable.addCell(cell("1", TextAlignment.CENTER));
            serviceTable.addCell(cell(invoice.getPurchaseOrder().getTrainingDetails(), TextAlignment.LEFT));
            serviceTable.addCell(cell("4 Hours", TextAlignment.CENTER));
            serviceTable.addCell(cell(
                    invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MMM-yy")),
                    TextAlignment.CENTER));
            serviceTable.addCell(cell("₹" + String.format("%.2f", invoice.getSubtotal()), TextAlignment.RIGHT));

            document.add(serviceTable);
            document.add(new Paragraph("\n"));

            // =====================================================
            // TOTAL / TDS / NET PAYABLE
            // =====================================================
            Table amountTable = new Table(2);
            amountTable.setWidth(UnitValue.createPercentValue(40));
            amountTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);

            amountTable.addCell(amountLabel("Subtotal"));
            amountTable.addCell(amountValue(invoice.getSubtotal()));

            amountTable.addCell(amountLabel("GST @ 18%"));
            amountTable.addCell(amountValue(invoice.getGstAmount()));



            Cell netLabel = new Cell()
                    .add(new Paragraph("Net Payable").setBold())
                    .setBackgroundColor(new DeviceRgb(232, 245, 233))
                    .setBorder(new SolidBorder(SECONDARY_COLOR, 1));



            amountTable.addCell(netLabel);
            // Net payable value cell (was missing)
            amountTable.addCell(amountValue(invoice.getTotalAmount()));

            document.add(amountTable);
            document.add(new Paragraph("\n"));

            // Amount in words (display rupees and paise)
            try {
                String words = convertAmountToWords(invoice.getTotalAmount());
                document.add(new Paragraph("Amount (in words): " + words).setItalic());
                document.add(new Paragraph("\n"));
            } catch (Exception ignored) {
                // Non-critical: if conversion fails, continue without breaking PDF generation
            }

            // =====================================================
            // BANK DETAILS
            // =====================================================
            document.add(new Paragraph("Bank Details for Payment")
                    .setBold()
                    .setFontColor(PRIMARY_COLOR));

            document.add(new Paragraph("Bank Name: " + invoice.getOurCompany().getBankName()));
            document.add(new Paragraph("Account Number: " + invoice.getOurCompany().getAccountNumber()));
            document.add(new Paragraph("IFSC Code: " + invoice.getOurCompany().getIfscCode()));
            document.add(new Paragraph("Account Holder: " + invoice.getOurCompany().getCompanyName()));

            document.add(new Paragraph("\n"));

            // =====================================================
            // FOOTER
            // =====================================================
            document.add(new Paragraph(
                    "Note: TDS Certificate will be issued within 15 days of payment.")
                    .setFontSize(9));

            document.add(new Paragraph("\n\n"));

            document.add(new Paragraph("For " + invoice.getOurCompany().getCompanyName())
                    .setBold());

            document.add(new Paragraph("Authorized Signatory"));

            document.close();
            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Error generating invoice PDF", e);
        }
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================
    private Cell header(String text) {
        return new Cell()
                .setBackgroundColor(PRIMARY_COLOR)
                .setFontColor(WHITE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(text));
    }

    private Cell cell(String text, TextAlignment align) {
        return new Cell()
                .setBackgroundColor(LIGHT_GRAY)
                .setTextAlignment(align)
                .add(new Paragraph(text));
    }

    private Cell amountLabel(String text) {
        return new Cell()
                .setBackgroundColor(LIGHT_GRAY)
                .add(new Paragraph(text));
    }

    private Cell amountValue(double value) {
        return new Cell()
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("₹" + String.format("%.2f", value)));
    }

    // Convert amount to words (handles rupees and paise)
    private String convertAmountToWords(double amount) {
        if (amount < 0) return "minus " + convertAmountToWords(-amount);
        long rupees = (long) amount;
        int paise = (int) Math.round((amount - rupees) * 100);

        String rupeesWords = (rupees == 0) ? "zero rupees" : numberToWords(rupees) + " rupees";
        String paiseWords = (paise == 0) ? "" : " and " + numberToWords(paise) + " paise";

        return capitalizeFirst(rupeesWords + paiseWords + " only");
    }

    private String numberToWords(long n) {
        if (n == 0) return "zero";
        final String[] ones = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
                "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
        final String[] tens = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};

        StringBuilder words = new StringBuilder();

        if (n >= 1_00_00_000) { // crores (Indian grouping)
            words.append(numberToWords(n / 1_00_00_000)).append(" crore ");
            n = n % 1_00_00_000;
        }
        if (n >= 1_00_000) { // lakhs
            words.append(numberToWords(n / 1_00_000)).append(" lakh ");
            n = n % 1_00_000;
        }
        if (n >= 1000) {
            words.append(numberToWords(n / 1000)).append(" thousand ");
            n = n % 1000;
        }
        if (n >= 100) {
            words.append(numberToWords(n / 100)).append(" hundred ");
            n = n % 100;
        }
        if (n > 0) {
            if (!words.isEmpty()) words.append("and ");
            if (n < 20) words.append(ones[(int) n]);
            else {
                words.append(tens[(int) (n / 10)]);
                if ((n % 10) > 0) words.append(" ").append(ones[(int) (n % 10)]);
            }
        }
        return words.toString().trim();
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
