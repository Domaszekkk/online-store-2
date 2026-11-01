package com.estore.invoice.pdf;

import com.estore.invoice.Document;
import com.estore.invoice.DocumentType;
import com.estore.invoice.PdfRendererPort;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PdfRendererAdapter implements PdfRendererPort {
    private static final String FONT_FAMILY = "DejaVu Sans";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("pl")).withZone(ZoneId.of("Europe/Warsaw"));
    private static final Map<String, String> PAYMENT_METHOD_LABELS = Map.of(
            "CARD", "Karta płatnicza",
            "BLIK", "BLIK",
            "TRANSFER", "Przelew",
            "CASH_ON_DELIVERY", "Płatność przy odbiorze");
    private static final Set<String> PREPAID_PAYMENT_METHODS = Set.of("CARD", "BLIK");
    private static final Map<String, String> VAT_RATE_LABELS = Map.of(
            "VAT_23", "23%",
            "VAT_8", "8%",
            "VAT_5", "5%",
            "VAT_0", "0%");

    private final ITemplateEngine templateEngine;
    private final byte[] regularFont = readFont("fonts/DejaVuSans.ttf");
    private final byte[] boldFont = readFont("fonts/DejaVuSans-Bold.ttf");

    @Override
    public byte[] render(Document document) {
        Context context = new Context(Locale.forLanguageTag("pl"));
        context.setVariable("document", document);
        context.setVariable("title", document.getType() == DocumentType.INVOICE ? "Faktura VAT" : "Potwierdzenie zakupu");
        context.setVariable("invoice", document.getType() == DocumentType.INVOICE);
        context.setVariable("issueDate", DATE_FORMAT.format(document.getIssuedAt()));
        context.setVariable("saleDate", DATE_FORMAT.format(document.getSaleDate()));
        context.setVariable("paymentMethod", PAYMENT_METHOD_LABELS.getOrDefault(document.getPaymentMethod(), document.getPaymentMethod()));
        context.setVariable("vatRateLabels", VAT_RATE_LABELS);
        context.setVariable("totalLabel", PREPAID_PAYMENT_METHODS.contains(document.getPaymentMethod()) ? "Wartość zamówienia" : "Do zapłaty");
        String html = templateEngine.process("document", context);
        return toPdf(html);
    }

    private byte[] toPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                    .useFastMode()
                    .useFont(() -> new ByteArrayInputStream(regularFont), FONT_FAMILY, 400, FontStyle.NORMAL, true)
                    .useFont(() -> new ByteArrayInputStream(boldFont), FONT_FAMILY, 700, FontStyle.NORMAL, true)
                    .withHtmlContent(html, null)
                    .toStream(output)
                    .run();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Rendering the document PDF failed", exception);
        }
    }

    private static byte[] readFont(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Font " + path + " is missing on the classpath", exception);
        }
    }
}
