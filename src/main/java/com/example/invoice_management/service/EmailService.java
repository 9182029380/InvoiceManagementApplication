package com.example.invoice_management.service;

import com.example.invoice_management.entity.Invoice;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendInvoiceEmail(Invoice invoice, String pdfPath) {
        try {
            // Validate PDF file exists
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                throw new RuntimeException("Invoice PDF file not found at: " + pdfPath);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String clientEmail = invoice.getPurchaseOrder().getClientCompany().getEmail();
            if (clientEmail == null || clientEmail.isEmpty()) {
                throw new RuntimeException("Client email is not set for company: " +
                    invoice.getPurchaseOrder().getClientCompany().getCompanyName());
            }

            String subject = "Invoice " + invoice.getInvoiceNumber() + " - "
                    + invoice.getOurCompany().getCompanyName();

            String contactPerson = invoice.getOurCompany().getContactPerson() != null
                ? invoice.getOurCompany().getContactPerson()
                : "Sales Team";

            String body = String.format("""
                Dear %s,

                We are pleased to inform you that the training services as outlined in the Purchase Order (PO) have been successfully completed.

                As discussed, we are raising the invoice in accordance with the agreed terms and conditions mentioned in the PO. Kindly find the attached invoice for your reference and initiate the payment process as per the PO agreement.

                Invoice Details:
                Purchase Order Number: %s
                Invoice Number: %s
                Invoice Date: %s
                Total Amount: ₹%.2f

                Please let us know if any additional information or documentation is required from our end to proceed with the payment.

                Thank you for your continued support and cooperation.

                Warm regards,
                %s
                %s
                📞 +91 91820 92380
                """,
                    invoice.getPurchaseOrder().getClientCompany().getCompanyName(),
                    invoice.getPurchaseOrder().getPoNumber(),
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceDate(),
                    invoice.getTotalAmount(),
                    contactPerson,
                    invoice.getOurCompany().getCompanyName()
            );

            helper.setTo(clientEmail);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(pdfFile.getName(), pdfFile);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Error sending email: " + e.getMessage(), e);
        }
    }
}

