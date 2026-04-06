package com.example.project_backend04.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.print.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class InvoicePrintService {

    private final TemplateEngine templateEngine;

    /**
     * Generate PDF from HTML template
     */
    public byte[] generateInvoicePdf(Object data) {
        try {
            // 1. Bind data to template
            Context context = new Context();
            context.setVariable("invoice", data);

            // 2. Render HTML
            String html = templateEngine.process("invoice-template", context);

            // 3. Convert HTML → PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating invoice PDF", e);
        }
    }

    /**
     * Print PDF directly to default printer
     */
    public void printPdf(byte[] pdfBytes) {
        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();

            if (printService == null) {
                throw new RuntimeException("No printer found");
            }

            DocPrintJob job = printService.createPrintJob();

            Doc doc = new SimpleDoc(
                    new ByteArrayInputStream(pdfBytes),
                    DocFlavor.INPUT_STREAM.PDF,
                    null
            );

            job.print(doc, null);

        } catch (Exception e) {
            throw new RuntimeException("Error printing PDF", e);
        }
    }

    /**
     * Full flow: generate + print
     */
    public void generateAndPrint(Object data) {
        byte[] pdf = generateInvoicePdf(data);
        printPdf(pdf);
    }
}
