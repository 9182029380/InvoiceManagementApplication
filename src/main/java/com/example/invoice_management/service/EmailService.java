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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String clientEmail = invoice.getPurchaseOrder().getClientCompany().getEmail();
            String subject = "Invoice " + invoice.getInvoiceNumber() + " - "
                    + invoice.getOurCompany().getCompanyName();

            String body = String.format("""
                Dear %s,
                
                Please find attached the invoice for the training services as per PO Number: %s
                
                Invoice Details:
                Invoice Number: %s
                Invoice Date: %s
                Amount: ₹%.2f
                
                Please process the payment at your earliest convenience.
                
                Bank Details:
                Bank Name: %s
                Account Number: %s
                IFSC Code: %s
                
                Thank you for your business!
                
                Best Regards,
                %s
                %s
                """,
                    invoice.getPurchaseOrder().getClientCompany().getCompanyName(),
                    invoice.getPurchaseOrder().getPoNumber(),
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceDate(),
                    invoice.getTotalAmount(),
                    invoice.getOurCompany().getBankName(),
                    invoice.getOurCompany().getAccountNumber(),
                    invoice.getOurCompany().getIfscCode(),
                    invoice.getOurCompany().getCompanyName(),
                    invoice.getOurCompany().getEmail()
            );

            helper.setTo(clientEmail);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(new File(pdfPath).getName(), new File(pdfPath));

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }
}

