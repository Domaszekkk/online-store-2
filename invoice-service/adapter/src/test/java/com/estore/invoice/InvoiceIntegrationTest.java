package com.estore.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private KafkaContainer kafkaContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String COMPANY_BILLING_ADDRESS = """
            {"companyName":"Kowalski Sp. z o.o.","taxIdentificationNumber":"5260250274",
             "street":"Firmowa 5","city":"Kraków","postalCode":"30-001","country":"Polska"}""";

    private String orderPlacedEvent(UUID orderId, String billingAddress) {
        return """
                {"eventId":"%s","orderId":"%s","orderNumber":"ORD-2026-000042","placedAt":"2026-10-16T09:59:00Z",
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "billingAddress":%s,
                 "paymentMethod":"BLIK","deliveryMethod":"COURIER",
                 "deliveryCost":{"amount":19.99,"currency":"PLN"},"deliveryVatRate":"VAT_23",
                 "lines":[{"productId":"%s","variantId":"%s","productName":"ProBook X15","stockKeepingUnit":"PBX15-I7-16",
                           "unitPrice":{"amount":4599.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":2,
                           "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},
                                      "vatRate":"VAT_8","quantity":1}],
                           "lineTotal":{"amount":9237.00,"currency":"PLN"}}],
                 "itemsTotal":{"amount":9237.00,"currency":"PLN"},"grandTotal":{"amount":9256.99,"currency":"PLN"}}
                """.formatted(UUID.randomUUID(), orderId, billingAddress, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private void publish(UUID orderId, String payload) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("order-placed", orderId.toString(), payload);
        record.headers().add("eventType", "OrderPlaced".getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get();
    }

    private JsonNode awaitDocumentDetails(UUID orderId) throws Exception {
        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline)) {
            MvcResult result = mockMvc.perform(get("/documents/order/{orderId}/details", orderId)).andReturn();
            if (result.getResponse().getStatus() == 200) {
                return objectMapper.readTree(result.getResponse().getContentAsString());
            }
            Thread.sleep(300);
        }
        return fail("No document was issued for order " + orderId);
    }

    private String pdfText(UUID orderId) throws Exception {
        byte[] pdf = mockMvc.perform(get("/documents/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void orderPlacedWithCompanyBillingAddress_IssuesInvoiceWithPdf() throws Exception {
        UUID orderId = UUID.randomUUID();

        publish(orderId, orderPlacedEvent(orderId, COMPANY_BILLING_ADDRESS));

        JsonNode details = awaitDocumentDetails(orderId);
        String text = pdfText(orderId);
        assertAll(
                () -> assertEquals("INVOICE", details.get("type").asText()),
                () -> assertTrue(details.get("number").asText().matches("FV/\\d{4}/\\d{2}/\\d{6}")),
                () -> assertEquals("Kowalski Sp. z o.o.", details.get("buyer").get("companyName").asText()),
                () -> assertEquals(3, details.get("lines").size()),
                () -> assertEquals(2, details.get("vatSummaries").size()),
                () -> assertEquals(0, details.get("grossTotal").get("amount").decimalValue().compareTo(new java.math.BigDecimal("9256.99"))),
                () -> assertEquals(0, details.get("netTotal").get("amount").decimalValue().compareTo(new java.math.BigDecimal("7530.41"))),
                () -> assertTrue(text.contains("Faktura VAT"), text),
                () -> assertTrue(text.contains(details.get("number").asText()), text),
                () -> assertTrue(text.contains("Kowalski Sp. z o.o."), text),
                () -> assertTrue(text.contains("NIP: 5260250274"), text),
                () -> assertTrue(text.contains("Kraków"), text),
                () -> assertTrue(text.contains("Dostawa kurierem"), text),
                () -> assertTrue(text.contains("Wartość zamówienia: 9 256,99 zł"), text),
                () -> assertTrue(text.contains("Polska"), text)
        );
        ConsumerRecord<String, String> documentIssued = awaitRecord("document-issued", orderId.toString());
        JsonNode event = objectMapper.readTree(documentIssued.value());
        assertAll(
                () -> assertEquals("DocumentIssued",
                        new String(documentIssued.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8)),
                () -> assertEquals(details.get("number").asText(), event.get("documentNumber").asText()),
                () -> assertEquals("INVOICE", event.get("documentType").asText())
        );
    }

    private ConsumerRecord<String, String> awaitRecord(String topic, String key) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plusSeconds(30);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        return fail("No record with key " + key + " was published to " + topic);
    }

    @Test
    void orderPlacedWithoutBillingAddress_IssuesPurchaseConfirmation() throws Exception {
        UUID orderId = UUID.randomUUID();

        publish(orderId, orderPlacedEvent(orderId, "null"));

        JsonNode details = awaitDocumentDetails(orderId);
        String text = pdfText(orderId);
        assertAll(
                () -> assertEquals("PURCHASE_CONFIRMATION", details.get("type").asText()),
                () -> assertTrue(details.get("number").asText().startsWith("PZ/")),
                () -> assertTrue(details.get("buyer").isNull()),
                () -> assertTrue(text.contains("Potwierdzenie zakupu"), text),
                () -> assertTrue(text.contains("nie jest fakturą VAT"), text)
        );
    }

    @Test
    void sameOrderPlacedEventDeliveredTwice_IssuesOnlyOneDocument() throws Exception {
        UUID orderId = UUID.randomUUID();
        String payload = orderPlacedEvent(orderId, COMPANY_BILLING_ADDRESS);
        publish(orderId, payload);
        String firstNumber = awaitDocumentDetails(orderId).get("number").asText();
        UUID markerOrderId = UUID.randomUUID();

        publish(orderId, payload);
        publish(markerOrderId, orderPlacedEvent(markerOrderId, COMPANY_BILLING_ADDRESS));
        awaitDocumentDetails(markerOrderId);

        assertEquals(firstNumber, awaitDocumentDetails(orderId).get("number").asText());
    }

    @Test
    void malformedEvent_IsMovedToDeadLetterTopic() throws Exception {
        String key = UUID.randomUUID().toString();
        ProducerRecord<String, String> record = new ProducerRecord<>("order-placed", key, "{not json");
        record.headers().add("eventType", "OrderPlaced".getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of("order-placed-dlt"));
            Instant deadline = Instant.now().plusSeconds(30);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> deadLetter : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(deadLetter.key())) {
                        assertEquals("{not json", deadLetter.value());
                        return;
                    }
                }
            }
        }
        fail("Malformed event was not moved to the dead letter topic");
    }
}
