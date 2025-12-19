package com.example.invoice_management.controller;

import com.example.invoice_management.entity.*;
import com.example.invoice_management.repository.InvoiceRepository;
import com.example.invoice_management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
public class WebController {
    private final CompanyService companyService;
    private final PurchaseOrderService poService;
    private final InvoiceService invoiceService;
    private final PDFService pdfService;
    private final EmailService emailService;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            model.addAttribute("ourCompany", companyService.getOurCompany());
        } catch (Exception e) {
            model.addAttribute("needSetup", true);
        }
        model.addAttribute("poList", poService.getAllPurchaseOrders());
        model.addAttribute("invoiceList", invoiceService.getAllInvoices());
        return "dashboard";
    }

    // Company Management
    @GetMapping("/company/setup")
    public String setupCompany(Model model) {
        model.addAttribute("company", new OurCompany());
        return "company-setup";
    }

    @PostMapping("/company/setup")
    public String saveCompany(@ModelAttribute OurCompany company,
                              RedirectAttributes redirectAttributes) {
        companyService.createOurCompany(company);
        redirectAttributes.addFlashAttribute("success", "Company setup completed!");
        return "redirect:/web/";
    }

    @GetMapping("/client/new")
    public String newClient(Model model) {
        model.addAttribute("client", new ClientCompany());
        return "client-form";
    }

    @PostMapping("/client/save")
    public String saveClient(@ModelAttribute ClientCompany client,
                             RedirectAttributes redirectAttributes) {
        companyService.createClientCompany(client);
        redirectAttributes.addFlashAttribute("success", "Client added successfully!");
        return "redirect:/web/po/new";
    }

    // Purchase Order Management
    @GetMapping("/po/new")
    public String newPO(Model model) {
        model.addAttribute("po", new PurchaseOrder());
        model.addAttribute("clients", companyService.getAllClientCompanies());
        return "po-form";
    }

    @PostMapping("/po/save")
    public String savePO(@ModelAttribute PurchaseOrder po,
                         RedirectAttributes redirectAttributes) {
        try {
            // Check if this is an update (PO already exists in DB) or a new creation
            boolean isExisting = po.getPoNumber() != null && !po.getPoNumber().isBlank();
            if (isExisting) {
                try {
                    poService.getPurchaseOrder(po.getPoNumber());
                    // PO exists -> update
                    poService.updatePurchaseOrder(po.getPoNumber(), po);
                    redirectAttributes.addFlashAttribute("success",
                            "Purchase Order updated successfully!");
                } catch (Exception e) {
                    // PO does not exist -> create new
                    poService.createPurchaseOrder(po);
                    redirectAttributes.addFlashAttribute("success",
                            "Purchase Order created successfully!");
                }
            } else {
                poService.createPurchaseOrder(po);
                redirectAttributes.addFlashAttribute("success",
                        "Purchase Order created successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/po/new";
        }
        return "redirect:/web/";
    }

    @GetMapping("/po/list")
    public String listPOs(Model model) {
        model.addAttribute("poList", poService.getAllPurchaseOrders());
        return "po-list";
    }

    // new: edit PO
    @GetMapping("/po/edit/{poNumber}")
    public String editPO(@PathVariable String poNumber, Model model, RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder po = poService.getPurchaseOrder(poNumber);
            model.addAttribute("po", po);
            model.addAttribute("clients", companyService.getAllClientCompanies());
            return "po-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/po/list";
        }
    }

    // new: delete PO (web)
    @PostMapping("/po/delete/{poNumber}")
    public String deletePO(@PathVariable String poNumber,
                           @RequestParam(defaultValue = "false") boolean force,
                           RedirectAttributes redirectAttributes) {
        try {
            poService.deletePurchaseOrder(poNumber, force);
            redirectAttributes.addFlashAttribute("success", "PO deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting PO: " + e.getMessage());
        }
        return "redirect:/web/po/list";
    }

    // Invoice Management
    @GetMapping("/invoice/generate")
    public String generateInvoiceForm(Model model) {
        model.addAttribute("poList", poService.getAllPurchaseOrders()
                .stream()
                .filter(po -> po.getStatus() == POStatus.PENDING)
                .toList());
        return "invoice-generate";
    }

    @PostMapping("/invoice/generate")
    public String generateInvoice(@RequestParam String poNumber,
                                  RedirectAttributes redirectAttributes) {
        try {
            OurCompany ourCompany = companyService.getOurCompany();
            Invoice invoice = invoiceService.generateInvoice(
                    ourCompany.getCompanyId(), poNumber);

            String pdfPath = pdfService.generateInvoicePDF(invoice);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

            redirectAttributes.addFlashAttribute("success",
                    "Invoice " + invoice.getInvoiceNumber() + " generated successfully!");
            redirectAttributes.addFlashAttribute("invoiceId", invoice.getId());

            return "redirect:/web/invoice/view/" + invoice.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/invoice/generate";
        }
    }

    @GetMapping("/invoice/view/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoice(id);
        model.addAttribute("invoice", invoice);
        return "invoice-view";
    }

    @PostMapping("/invoice/send/{id}")
    public String sendInvoice(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.getInvoice(id);
            emailService.sendInvoiceEmail(invoice, invoice.getPdfPath());

            // allow re-send: always mark SENT (keeps current behaviour)
            invoice.setStatus(InvoiceStatus.SENT);
            invoiceRepository.save(invoice);

            redirectAttributes.addFlashAttribute("success",
                    "Invoice sent successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error sending invoice: " + e.getMessage());
        }
        return "redirect:/web/invoice/view/" + id;
    }

    @GetMapping("/invoice/list")
    public String listInvoices(Model model) {
        model.addAttribute("invoiceList", invoiceService.getAllInvoices());
        return "invoice-list";
    }

    // new: download invoice (web)
    @GetMapping("/invoice/download/{id}")
    public void downloadInvoice(@PathVariable Long id, HttpServletResponse response) {
        try {
            Invoice invoice = invoiceService.getInvoice(id);
            if (invoice.getPdfPath() == null) {
                String pdfPath = pdfService.generateInvoicePDF(invoice);
                invoice.setPdfPath(pdfPath);
                invoiceRepository.save(invoice);
            }

            File file = new File(invoice.getPdfPath());
            if (!file.exists()) throw new RuntimeException("Invoice PDF not found: " + invoice.getPdfPath());

            response.setContentType("application/pdf");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");

            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.flush();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error downloading invoice: " + e.getMessage());
        }
    }
}
