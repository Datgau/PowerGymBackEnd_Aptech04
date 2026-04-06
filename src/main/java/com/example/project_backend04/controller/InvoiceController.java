package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.InvoiceData;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.service.InvoicePrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoicePrintService invoicePrintService;
    private final PaymentOrderRepository paymentOrderRepository;

    @GetMapping("/{paymentOrderId}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String paymentOrderId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(paymentOrderId)
            .orElseThrow(() -> new RuntimeException("Payment order not found: " + paymentOrderId));

        InvoiceData invoiceData = InvoiceData.fromPaymentOrder(paymentOrder);

        byte[] pdfBytes = invoicePrintService.generateInvoicePdf(invoiceData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + paymentOrderId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }


    @PostMapping("/{paymentOrderId}/print")
    public ResponseEntity<String> printInvoice(@PathVariable String paymentOrderId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(paymentOrderId)
            .orElseThrow(() -> new RuntimeException("Payment order not found: " + paymentOrderId));

        InvoiceData invoiceData = InvoiceData.fromPaymentOrder(paymentOrder);

        invoicePrintService.generateAndPrint(invoiceData);

        return ResponseEntity.ok("Invoice printed successfully");
    }
}
