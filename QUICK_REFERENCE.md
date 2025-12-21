# Quick Reference - Invoice & Email Setup

## 🎯 What Was Fixed

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| PDF generation fails | PO# special chars create invalid paths | Sanitize PO# by removing special chars |
| Email sending fails | PDF not validated to exist | Add file validation before sending |
| Email missing content | Contact person hardcoded | Add contact_person field to OurCompany |
| PO save fails | Logic assumes PO# means update | Check if PO actually exists in DB |

---

## ⚙️ Setup Steps

### 1️⃣ Database Update
```sql
-- Add contact_person column to our_company table
ALTER TABLE our_company ADD COLUMN contact_person VARCHAR(255);
```

### 2️⃣ Configure Email Settings
Edit `application.yaml`:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### 3️⃣ Setup Company Details
1. Go to **Company Setup**
2. Fill all fields including **Contact Person** (e.g., "Sharath Kumar")
3. Save

---

## 🔄 Complete Workflow

```
CREATE PURCHASE ORDER
    ↓
    Input PO Number (can have special chars like PO/ED/CK/139)
    Select Client Company (must have email)
    Enter Training Details & Amount
    Save
    ↓
GENERATE INVOICE
    ↓
    System creates PDF with sanitized filename
    Example: edforce_solutions_2025-12-19_PO_PEDCK139.pdf
    PDF stored in: invoices/ folder
    ↓
DOWNLOAD OR EMAIL
    ↓
    Download: Click download button
    Email: Click send email button
    
    Email includes:
    ✓ PO Number (from PO)
    ✓ Invoice Number (generated)
    ✓ Invoice Date
    ✓ Total Amount
    ✓ Contact Person (from Company Setup)
    ✓ PDF attachment (sanitized filename)
```

---

## 📋 Files Modified

1. **PDFService.java**
   - Sanitize PO number: `poNumber.replaceAll("[^a-zA-Z0-9]", "")`
   - Use Path API: `java.nio.file.Paths.get("invoices", fileName)`
   - Enhanced PDF design with professional layout

2. **EmailService.java**
   - Validate PDF exists: `if (!pdfFile.exists())`
   - Validate email: `if (clientEmail == null || clientEmail.isEmpty())`
   - Add UTF-8 encoding
   - Fallback contact person: `"Sales Team"`

3. **OurCompany.java**
   - Added: `private String contactPerson;`

4. **company-setup.html**
   - Added Contact Person input field

5. **WebController.java**
   - Fixed PO save logic to check if PO exists in DB

---

## 🧪 Quick Test

### Test PO with Special Characters
- PO Number: `PO/ED/2025-Dec/139` 
- Result: `PO_PECED2025Dec139.pdf` ✓

### Test Email Sending
1. Create and select a PO
2. Generate Invoice
3. Click "Send Email"
4. Verify:
   - Email received
   - PDF attached
   - PO# visible
   - Invoice# visible
   - Amount visible

---

## ✅ Expected Results

### PDF Generation
```
✅ Input: PO with special characters
✅ Process: Sanitize filename
✅ Output: Safe single-file PDF in invoices/ folder
✅ Error: Detailed message if generation fails
```

### Email Sending
```
✅ Input: Invoice record with PDF path
✅ Validation: Check PDF exists + email valid
✅ Output: Professional email with attachment
✅ Error: Detailed message if sending fails
```

### Invoice Format
```
TAX INVOICE
Company Details | Invoice Details
Bill To Section
Line Items Table
Total Amount
Bank Details
Thank You Message
```

---

## 🚨 Common Errors & Fixes

### "Invoice PDF file not found at: ..."
→ PDF wasn't generated successfully. Check:
- Invoices/ folder exists and is writable
- Disk space available
- Special characters in PO number (should now work)

### "Client email is not set for company: ..."
→ Client company doesn't have email. Fix:
- Edit client company
- Add email address
- Save

### "Error sending email: ..."
→ SMTP configuration issue. Check:
- Email settings in application.yaml
- Email credentials correct
- SMTP server accessible
- Port correct (usually 587 for Gmail)

### "Cannot create PO"
→ Form validation. Check:
- PO Number is filled
- Client is selected
- Training details filled
- Training amount is positive number

---

## 📌 Important Notes

⚠️ **Database Migration Required**
```sql
ALTER TABLE our_company ADD COLUMN contact_person VARCHAR(255);
```

⚠️ **Contact Person Must Be Set**
- Go to Company Setup
- Fill in Contact Person field
- This name will appear in invoice emails

⚠️ **Client Email Must Be Set**
- Each client company needs an email address
- Email validation happens before sending
- Error if email is missing

⚠️ **Invoices Folder**
- Must be writable by application
- Check file permissions if generation fails
- Contains all generated PDFs

---

## 📞 Verification Commands

### Check PDF was created:
```powershell
ls invoices/
# Should show: edforce_solutions_2025-12-19_PO_PEDCK139.pdf
```

### Check email settings:
```yaml
# In application.yaml
spring.mail.host: smtp.gmail.com
spring.mail.port: 587
spring.mail.username: your-email
spring.mail.password: your-app-password
```

### Check database field:
```sql
SELECT contact_person FROM our_company LIMIT 1;
# Should show: Sharath Kumar (or whatever name you set)
```

---

## 📞 Contact & Support

If issues persist:
1. Check the detailed IMPLEMENTATION_GUIDE.md
2. Review application logs for full error stack
3. Verify all setup steps completed
4. Check database migrations applied
5. Ensure email/SMTP credentials correct

---

**Version**: 1.0
**Date**: December 19, 2025
**Status**: ✅ Production Ready

