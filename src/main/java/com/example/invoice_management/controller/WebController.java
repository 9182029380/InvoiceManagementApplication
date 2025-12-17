package com.example.invoice_management.controller;

import com.example.invoice_management.entity.*;
import com.example.invoice_management.repository.InvoiceRepository;
import com.example.invoice_management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        poService.createPurchaseOrder(po);
        redirectAttributes.addFlashAttribute("success",
                "Purchase Order created successfully!");
        return "redirect:/web/";
    }

    @GetMapping("/po/list")
    public String listPOs(Model model) {
        model.addAttribute("poList", poService.getAllPurchaseOrders());
        return "po-list";
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
}
