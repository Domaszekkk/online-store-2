package com.estore.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderPlacedEventPublishingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private KafkaContainer kafkaContainer;
    @MockitoBean
    private CartPort cartPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private UUID checkoutReturnsLaptopWithCable() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L)).thenReturn(CartDetails.builder()
                .id(cartId)
                .items(List.of(CartItemDetails.builder()
                        .productId(UUID.randomUUID()).variantId(UUID.randomUUID())
                        .productName("ProBook X15").stockKeepingUnit("PBX15-I7-16")
                        .unitPrice(pln("4599.00")).vatRate(VatRate.VAT_23).quantity(2)
                        .addOns(List.of(CartItemAddOnDetails.builder()
                                .productId(UUID.randomUUID()).name("Kabel HDMI 2m").price(pln("39.00")).vatRate(VatRate.VAT_8).quantity(1)
                                .build()))
                        .build()))
                .build());
        return cartId;
    }

    private KafkaConsumer<String, String> consumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private ConsumerRecord<String, String> awaitRecordWithKey(String key) {
        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of("order-placed"));
            Instant deadline = Instant.now().plusSeconds(30);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        return fail("No OrderPlaced event with key " + key + " was published");
    }

    @Test
    void placeOrder_DataCorrect_PublishesOrderPlacedEventFromOutbox() throws Exception {
        UUID cartId = checkoutReturnsLaptopWithCable();
        String body = """
                {"cartId":"%s","cartVersion":1,
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "billingAddress":{"companyName":"Kowalski Sp. z o.o.","taxIdentificationNumber":"5260250274",
                                   "street":"Firmowa 5","city":"Kraków","postalCode":"30-001","country":"Polska"},
                 "paymentMethod":"BLIK","deliveryMethod":"COURIER"}
                """.formatted(cartId);
        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode order = objectMapper.readTree(response);
        String orderId = order.get("id").asText();

        ConsumerRecord<String, String> record = awaitRecordWithKey(orderId);

        JsonNode event = objectMapper.readTree(record.value());
        assertAll(
                () -> assertEquals("OrderPlaced",
                        new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8)),
                () -> assertNotNull(record.headers().lastHeader("eventId")),
                () -> assertEquals(orderId, event.get("orderId").asText()),
                () -> assertEquals(order.get("orderNumber").asText(), event.get("orderNumber").asText()),
                () -> assertEquals("5260250274", event.get("billingAddress").get("taxIdentificationNumber").asText()),
                () -> assertEquals("VAT_23", event.get("lines").get(0).get("vatRate").asText()),
                () -> assertEquals("VAT_8", event.get("lines").get(0).get("addOns").get(0).get("vatRate").asText()),
                () -> assertEquals("VAT_23", event.get("deliveryVatRate").asText()),
                () -> assertEquals(0, new BigDecimal("9256.99").compareTo(event.get("grandTotal").get("amount").decimalValue()))
        );
    }

    @Test
    void documentIssuedEvent_OrderPlaced_ConfirmsOrderWithDocumentNumber() throws Exception {
        UUID cartId = checkoutReturnsLaptopWithCable();
        String body = """
                {"cartId":"%s","cartVersion":1,
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "paymentMethod":"BLIK","deliveryMethod":"COURIER"}
                """.formatted(cartId);
        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(response).get("id").asText();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            ProducerRecord<String, String> record = new ProducerRecord<>("document-issued", orderId,
                    "{\"orderId\":\"%s\",\"documentNumber\":\"PZ/2026/10/000007\"}".formatted(orderId));
            record.headers().add("eventType", "DocumentIssued".getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        }

        Instant deadline = Instant.now().plusSeconds(30);
        JsonNode order = null;
        while (Instant.now().isBefore(deadline)) {
            order = objectMapper.readTree(mockMvc.perform(get("/orders/{id}", orderId))
                    .andReturn().getResponse().getContentAsString());
            if ("CONFIRMED".equals(order.get("status").asText())) {
                break;
            }
            Thread.sleep(200);
        }
        JsonNode confirmedOrder = order;
        assertAll(
                () -> assertEquals("CONFIRMED", confirmedOrder.get("status").asText()),
                () -> assertEquals("PZ/2026/10/000007", confirmedOrder.get("documentNumber").asText())
        );
    }
}
