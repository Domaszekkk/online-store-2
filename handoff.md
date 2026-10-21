# Handoff — Mikroserwisowy System Sklepu Internetowego

> Monorepo: `online-store-2`. Java 21, Spring Boot 4.1.1, architektura heksagonalna, Docker, Postgres, Liquibase, Feign, Kafka (KRaft), Swagger.
> Reference wzorca architektury: `C:\Users\mkorc\programy\medical-clinic` (heksagon, ale Boot 3.5 — **nie kopiować z niego wersji bibliotek**).

---

## 1. Goal

Zbudować kompletny system sklepu internetowego w architekturze **mikroserwisowej** na poziomie komercyjnym (senior+). Mikroserwisy:
- **Produktów** (Task 1) — katalog, generyczny model produktu wzorowany na Shopify (options/variants).
- **Koszyka** (Task 2) — koszyk zakupowy.
- **Zamówień** (Task 3) — składanie zamówień + historia.
- **Fakturowanie** (Task 4) — generowanie faktur przez Kafkę.
- **Interakcji z Użytkownikiem** (Task 5) — BFF/proxy, jedyny punkt wejścia dla frontendu.
- **Docker Compose + prezentacja** (Task 6).

Każdy serwis: **Maven multi-module** (`model → domain → public → adapter`), **własna baza Postgres**, Liquibase, Swagger, MapStruct, testy (unit / web-slice / integracja Testcontainers). Komunikacja synchroniczna przez **Feign**, asynchroniczna przez **Kafkę** (outbox). Snapshot cen (koszyk/zamówienie pamiętają cenę i stawkę VAT z chwili dodania). UUID jako ID. `Money` jako value object. Ceny są brutto.

Pełny model danych i uzasadnienie: `MODEL-DANYCH.md`.

---

## 2. Current State

**✅ Task 1 — product-service — GOTOWY** (port `8080`, baza `5432`).
Endpointy: `POST /products`, `GET /products`, `GET /products?type=`, `GET /products/{id}`, `GET /products/{id}/configuration`, `PUT /products/{id}` (pełny: także options/variants/addOns), `DELETE /products/{id}`. Wariant i add-on mają `vatRate` (wymagane).
Testy: domena 16/16, kontroler (web-slice) 12/12, integracja (Testcontainers) 9/9.

**✅ Task 2 — cart-service — GOTOWY** (port `8081`, baza `5433`).
Endpointy: `POST /carts`, `GET /carts/{id}`, `POST /carts/{id}/items` (Feign → product-service: snapshot ceny/VAT/SKU/nazwy, sprawdzanie dostępności; ta sama konfiguracja = zwiększenie ilości), `PATCH /carts/{id}/items/{itemId}`, `DELETE /carts/{id}/items/{itemId}`, `POST /carts/{id}/checkout` (zamyka koszyk → `CHECKED_OUT`, zwraca zawartość; pusty/już zamknięty → 409).
Testy: domena 23/23, kontroler 14/14, integracja 7/7, adapter Feign (stub HTTP z JDK) 4/4.

**✅ Task 3 — order-service — GOTOWY** (port `8082`, baza `5434`).
Endpointy: `POST /orders` (Feign → `POST /carts/{id}/checkout`, snapshot pozycji z add-onami i VAT, koszt dostawy, numer `ORD-<rok>-<nr>`, opcjonalny `billingAddress` z `companyName` + `taxIdentificationNumber`), `GET /orders/{id}`, `GET /orders?email=`.
Testy: domena 13/13, kontroler 13/13, integracja 10/10, publikacja zdarzenia do Kafki (Testcontainers) 1/1, adapter Feign (stub HTTP: ponowienia po 503 i timeoucie) 6/6.

**✅ Task 4 — fakturowanie przez Kafkę — GOTOWY** (gałąź `feature/ESS-004`, 2026-10-16).
- order-service zapisuje `OrderPlaced` do tabeli outbox w transakcji zamówienia; publisher wysyła na topic `order-placed`.
- **invoice-service** (port `8083`, baza `5435`) konsumuje zdarzenie i wystawia **fakturę** (`FV/…`, gdy jest `billingAddress`) albo **potwierdzenie zakupu** (`PZ/…`); PDF zapisany w bazie.
- Endpointy: `GET /documents/order/{orderId}` (PDF, `inline; filename="faktura-FV-2026-10-000001.pdf"`), `GET /documents/order/{orderId}/details` (JSON).
- Testy invoice-service: domena 8/8, kontroler 5/5, integracja (Postgres + Kafka w Testcontainers: faktura dla firmy z odczytem tekstu PDF, potwierdzenie zakupu, duplikat zdarzenia → jeden dokument, błędna wiadomość → DLT) 4/4.
- **Zweryfikowane na żywo w `docker compose`**: migracje na istniejących danych (stare wiersze → `VAT_23`); zamówienie z fakturą na firmę → `FV/2026/10/000001` (9256,99 zł brutto); zamówienie bez adresu do faktury, paczkomat, karta → `PZ/2026/10/000001` (4650,99 zł); zły NIP → 400; nieznane zamówienie → 404; **odporność outboxa**: Kafka bez topiku → zdarzenie czekało w `outbox_events` i zostało wysłane po naprawie, dokument powstał; topiki przetrwały `--force-recreate` kontenera Kafki (wolumen).

**⏳ Task 5–6 — do zrobienia.** Następny: Task 5 (gateway-service).

Git: zarządza użytkownik ręcznie (nic nie commitowane automatycznie).

---

## 3. Active Files

```
online-store-2/
├─ docker-compose.yml            # product, cart, order, invoice (+ 4 bazy) + kafka (KRaft, wolumen kafka-data)
├─ MODEL-DANYCH.md               # specyfikacja modelu danych (z kodu, z Task 4)
├─ handoff.md                    # ten plik
├─ product-service/              # Task 1 (moduły: product-model/-domain/-public/-adapter)
│  ├─ pom.xml, Dockerfile, .mvn/ (wrapper 3.9.15), bruno/ (01–08)
│  ├─ model/   .../com/estore/product/   Product, ProductOption, ProductVariant (+vatRate), AddOn (+vatRate), Money, VatRate,
│  │                                     ProductType, Availability, AddProductCommand, UpdateProductCommand,
│  │                                     ProductException, ProductNotFoundException, InvalidAddOnException
│  ├─ domain/  ProductService, ProductRepositoryPort
│  ├─ public/  odpowiedzi: ProductDto, ProductOptionDto, ProductVariantDto, AddOnDto, MoneyDto, ErrorMessage
│  │           żądania (Bean Validation): AddProductRequest, UpdateProductRequest, ProductOptionRequest,
│  │                                       ProductVariantRequest, AddOnRequest, MoneyRequest
│  └─ adapter/ ProductController, *Entity, ProductJpaRepository, ProductRepositoryAdapter, ProductMapper,
│              ProductServiceConfig, GlobalExceptionHandler, OpenApiConfig, db/changelog (001, 002-add-vat-rate)
├─ cart-service/                 # Task 2 (moduły: cart-model/-domain/-public/-adapter)
│  ├─ pom.xml, Dockerfile, .mvn/, bruno/ (01–06)
│  ├─ model/   Cart (+checkout()), CartItem (+vatRate), CartItemAddOn (+quantity, +vatRate), Money, VatRate, CartStatus,
│  │           AddItemCommand, AddOnSelection, ChangeItemQuantityCommand, ProductDetails, VariantDetails, AddOnDetails,
│  │           wyjątki (…, EmptyCartException, ProductCatalogUnavailableException)
│  ├─ domain/  CartService, CartRepositoryPort, ProductCatalogPort
│  ├─ public/  CartDto, CartItemDto, CartItemAddOnDto, MoneyDto, ErrorMessage; AddItemRequest, AddOnSelectionRequest,
│  │           ChangeItemQuantityRequest
│  └─ adapter/ cart/ (controller, encje, repozytorium, mapper), client/ (ProductClient, rekordy odpowiedzi z vatRate,
│              ProductCatalogAdapter, ProductClientConfiguration, RetryOnServerErrorDecoder), db/changelog (… 004-add-vat-rate)
├─ order-service/                # Task 3 + Task 4 (moduły: order-model/-domain/-public/-adapter)
│  ├─ pom.xml, Dockerfile, .mvn/, bruno/ (01–05; 05 = zamówienie z fakturą na firmę)
│  ├─ model/   Order, OrderLine (+vatRate), OrderLineAddOn (+vatRate), Customer, Address, BillingAddress (companyName,
│  │           taxIdentificationNumber, requireValid(), isValidTaxIdentificationNumber()), Money, VatRate, OrderStatus,
│  │           PaymentMethod, DeliveryMethod (+vatRate()), PlaceOrderCommand, CartDetails/CartItemDetails/
│  │           CartItemAddOnDetails (+vatRate), wyjątki (…, InvalidBillingAddressException 400)
│  ├─ domain/  OrderService, OrderRepositoryPort, CartCheckoutPort, OrderEventPort, TransactionPort
│  ├─ public/  OrderDto, OrderLineDto, OrderLineAddOnDto, CustomerDto, AddressDto, BillingAddressDto, MoneyDto, ErrorMessage;
│  │           PlaceOrderRequest, CustomerRequest, AddressRequest, BillingAddressRequest,
│  │           @ValidTaxIdentificationNumber (+Validator), @CompleteCompanyData (+Validator)
│  └─ adapter/ order/  (controller, encje, BillingAddressEmbeddable, repozytorium, mapper, OrderServiceConfig,
│              │        SpringTransactionAdapter → TransactionTemplate)
│              client/ (CartClient, rekordy z vatRate, CartCheckoutAdapter, CartClientConfiguration, SafeRetryer)
│              event/  OrderPlacedEvent (record), OrderPlacedEventFactory, OutboxEventEntity, OutboxEventJpaRepository
│                      (FOR UPDATE SKIP LOCKED), OutboxOrderEventAdapter (implements OrderEventPort, propagation MANDATORY),
│                      OutboxPublisher (@Scheduled), OrderEventsConfig (NewTopic, @EnableScheduling)
│              db/changelog (… 003-add-vat-rate-and-company-billing, 004-create-outbox-events-table)
└─ invoice-service/              # Task 4 (moduły: invoice-model/-domain/-public/-adapter)
   ├─ pom.xml, Dockerfile, .mvn/, bruno/ (01 details, 02 PDF)
   ├─ model/   Document (issue(), typeFor()), DocumentLine, VatSummary, DocumentType (FV/PZ, formatNumber()), Seller, Buyer,
   │           Money, VatRate (netFromGross()), IssueDocumentCommand, OrderCustomer, OrderBillingAddress, OrderedLine,
   │           OrderedAddOn, DocumentException, DocumentNotFoundException
   ├─ domain/  DocumentService, DocumentRepositoryPort, DocumentNumberPort, PdfRendererPort
   ├─ public/  DocumentDto, DocumentLineDto, VatSummaryDto, SellerDto, BuyerDto, MoneyDto, ErrorMessage
   └─ adapter/ document/ DocumentController, DocumentEntity (+Seller-/BuyerEmbeddable), DocumentLineEntity, VatSummaryEntity,
               │         DocumentPdfEntity, repozytoria, DocumentRepositoryAdapter, DocumentNumberAdapter
               │         (INSERT … ON CONFLICT … RETURNING), DocumentMapper, DocumentServiceConfig, SellerProperties
               event/    OrderPlacedEvent (własna kopia), OrderPlacedEventMapper, OrderPlacedListener (@KafkaListener,
               │         @Transactional), OrderEventsConsumerConfig (DefaultErrorHandler + DLT order-placed-dlt)
               pdf/      PdfRendererAdapter (Thymeleaf → OpenHTMLToPDF)
               resources/ templates/document.html, fonts/DejaVuSans(-Bold).ttf, db/changelog/001-create-documents-tables
```

Kluczowe stałe wersje: Spring Boot **4.1.1**, springdoc **3.1.1**, MapStruct **1.6.3**, lombok-mapstruct-binding **0.2.0**, Testcontainers BOM **2.0.5**, Spring Cloud **2025.1.3**, Postgres **17**, Maven wrapper **3.9.15**, Kafka **apache/kafka:4.3.1** (compose i Testcontainers), openhtmltopdf **1.1.85** (PDFBox 3), font DejaVu Sans 2.37.

---

## 4. Changes Made

- **product-service**: pełny heksagon, generyczny model produktu (options/variants/addOns), `Availability` — **brak `stock`**. Tabele relacyjne + Liquibase. Swagger.
- **cart-service**: pełny heksagon, koszyk z pozycjami, snapshot cen, `lineTotal`/`total`. **Feign** `ProductClient` + `ProductCatalogPort`/`ProductCatalogAdapter`.
- **Nazewnictwo modułów**: unikalne `artifactId` per serwis (`<service>-model` itd.) — patrz sekcja 5.
- **docker-compose.yml**: 4 serwisy + 4 bazy (5432–5435) + `kafka` (porty: 9092 wewnątrz sieci compose, **9094 z hosta**). order-service czeka na healthy cart-service i kafka; invoice-service na swoją bazę i kafka.
- **Bruno**: `product-service/bruno` (01–08), `cart-service/bruno` (01–06), `order-service/bruno` (01–05), `invoice-service/bruno` (01–02).
- **Kod bez komentarzy**: Javadoc i `//` usunięte z klas (reguła użytkownika — nie dodawać ich w nowym kodzie, także w testach).

### Poprawki po review (2026-10-15)
- cart: reguły koszyka w modelu (`ACTIVE`, łączenie tej samej konfiguracji, walidacja ilości); add-on z własną ilością; `PATCH`/`DELETE` pozycji; odporność Feign (retry, timeouty, 503).
- Bean Validation (klasy żądań w `public`, `@Valid`, MapStruct `toCommand`, `GlobalExceptionHandler` 400).
- product: `UpdateProductCommand`, naprawa `PUT` kasującego atrybuty, pełny `PUT` (options/variants/addOns), walidacja add-onów.
- Build: procesory adnotacji w `annotationProcessorPaths`. Docker: cache `.m2`, `HEALTHCHECK`. Waluta: sztywne `PLN`.

### Task 3 — order-service (2026-10-16)
- Historia po emailu; enum statusów (używany `NEW`); checkout koszyka jednym wywołaniem; koszt dostawy w enumie; numer `ORD-<rok>-<6 cyfr>` z sekwencji; `billingAddress` opcjonalny (brak = brak faktury, adres dostawy nie jest kopiowany).
- Błędy z cart-service: 404 → 404; 409 → 409 z powodem; inne → 503.

### Task 4 — fakturowanie (2026-10-16)
- **Decyzje użytkownika**: osobny invoice-service; cena = brutto + `vatRate` na wariancie; `OrderPlaced` publikowany zawsze; brak `billingAddress` → potwierdzenie zakupu (paragonu nie udajemy — robi go kasa fiskalna); faktura na firmę/osobę wynika z danych (bez pola `buyerType`); transactional outbox; numeracja `FV|PZ/<rok>/<miesiąc>/<6 cyfr>`; dane sprzedawcy w `application.properties`; PDF generowany raz i zapisany w bazie; jedna tabela `documents` + endpointy `/documents/order/{orderId}` i `/details`.
- **Decyzje podjęte w trakcie (do akceptacji użytkownika)**:
  - `vatRate` także na **add-onie** (ma własną cenę) i w żądaniach **wymagane** (bez domyślnego 23%, bo `PUT` zastępuje warianty i po cichu zmieniłby stawkę). Stare wiersze w bazach → `VAT_23`.
  - Brak `vatRate` w odpowiedzi innego serwisu → `IllegalStateException` (kontrakt złamany), bez zgadywania stawki.
  - **VAT dostawy = 23%** (`DeliveryMethod.vatRate()`), przekazywany w zdarzeniu jako `deliveryVatRate`.
  - **Walidacja adresu do faktury przed checkoutem koszyka** (`OrderService.placeOrder` → `BillingAddress.requireValid()`), żeby zły NIP nie zostawił zamkniętego koszyka bez zamówienia.
  - Transakcja zamówienie + outbox przez port domeny `TransactionPort` (adapter: `TransactionTemplate`); wywołanie Feign do cart-service jest **poza** transakcją (nie trzyma połączenia z bazą).
  - Zdarzenie zawiera wszystko, czego potrzebuje faktura (klient, adres do faktury, pozycje z VAT, add-ony, dostawa, sumy) — invoice-service nie odpytuje order-service.
  - Jeden szablon `document.html` z warunkami (faktura / potwierdzenie) zamiast dwóch osobnych.
  - Konsument: `DefaultErrorHandler` (3 ponowienia co 1 s, potem DLT `order-placed-dlt`); błędy parsowania JSON / złe enumy — od razu do DLT bez ponawiania.
  - Kafka w compose: wolumen `kafka-data` + stały `CLUSTER_ID`, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` (topiki tworzą aplikacje przez `NewTopic`: `order-placed` 3 partycje — order-service, `order-placed-dlt` — invoice-service), healthcheck `nc -z localhost 9092`.
- **Krok po kroku**: (1) product `VatRate` → (2) cart snapshot VAT → (3) order snapshot VAT + `BillingAddress` z NIP → (4) outbox + Kafka → (5–7) invoice-service (model, numeracja, PDF, endpointy, Bruno). Każdy krok: testy + weryfikacja na żywo.

---

## 5. Failed Attempts / Gotchas (Spring Boot 4 — WAŻNE, nie powtarzać)

Boot 4 mocno zmodularyzował i przeszedł na Jackson 3. Dla kolejnych serwisów skopiować pom invoice-service (z Kafką) albo cart-service (z Feign) jako wzorzec.

1. **Maven wrapper 3.10.0 → IntelliJ „Unable to create injector" (Guice).** Fix: wrapper **3.9.15**.
2. **Testcontainers 2.0**: import `testcontainers-bom:2.0.5` + artefakty z prefiksem: `testcontainers-junit-jupiter`, `testcontainers-postgresql`, **`testcontainers-kafka`** (klasa `org.testcontainers.kafka.KafkaContainer`).
3. **`@AutoConfigureMockMvc` / `@WebMvcTest`** w `org.springframework.boot.webmvc.test.autoconfigure` + zależność **`spring-boot-starter-webmvc-test`**.
4. **Liquibase** wymaga modułu **`spring-boot-liquibase`** (sam `liquibase-core` cicho nic nie robi).
5. **Jackson 3** (`tools.jackson`) domyślny → w testach lokalny `new ObjectMapper()` (com.fasterxml).
6. **MapStruct + Lombok builder**: `addOns` jako adder → `@Mapper(builder=@Builder(disableBuilder=true))`.
7. **`@Builder.Default` + `@NoArgsConstructor`**: deserializacja bez pola → `null` → null-guard.
8. **Identyczne `artifactId` między serwisami** → kolizje. Fix: `<service>-model` itd.
9. **Unidirectional `@OneToMany @JoinColumn`** (także z `@OrderColumn`): kolumna FK i kolumna kolejności **muszą być nullable**.
10. **BigDecimal w asercjach**: `compareTo(...) == 0`.
11. **Feign na Boot 4**: Spring Cloud **2025.1.3**, `@EnableFeignClients`.
12. **Feign `Retryer` nie ponawia 5xx** bez własnego `ErrorDecoder`; konfiguracja klienta bez `@Configuration`.
13. **`annotationProcessorPaths`**: Lombok też musi być na liście.
14. **Kolejność w Bruno/testach**: najpierw akcesorium, potem produkt z add-onem.
15. **`@Builder.Default` w komendach update** czyści dane przy mapowaniu setterami — nie używać.
16. **Bean Validation w heksagonie**: adnotacje tylko w `public`; własne ograniczenia (`ConstraintValidator`) też w `public`, logika (np. suma kontrolna NIP) w `model`, żeby domena mogła jej użyć. Ograniczenie na poziomie klasy → `addPropertyNode(...)`, inaczej `GlobalExceptionHandler` (czyta `getFieldErrors()`) zwróci pusty komunikat.
17. **Nie ponawiać nieidempotentnych POST-ów Feignem.** Od 2026-10-20 `POST /carts/{id}/checkout` jest idempotentny, więc order-service ponawia go zwykłym `Retryer.Default` + `RetryOnServerErrorDecoder` (timeouty, błędy I/O, 5xx; 404/409 nigdy). `SafeRetryer` usunięty. Przy nowych POST-ach zawsze najpierw pytanie: czy ponowienie jest bezpieczne?
18. **ROZWIĄZANE 2026-10-20 (opcja B)** — wcześniej: checkout koszyka i zapis zamówienia to dwie transakcje w dwóch bazach; timeout po udanym checkoucie zostawiał koszyk `CHECKED_OUT` bez zamówienia, a ponowienie dawało 409 (zaobserwowane na żywo 2026-10-16). Teraz:
    - cart-service: ponowny checkout zamkniętego koszyka zwraca tę samą zawartość (bez zapisu, `updatedAt` bez zmian); 409 tylko dla pustego koszyka; zmiany pozycji w zamkniętym koszyku dalej 409.
    - order-service: jedno zamówienie na koszyk — `findByCartId` przed checkoutem → 409 `OrderAlreadyPlacedException` „Order ORD-… has already been placed from cart …” (bez wywołania cart-service); wyścig dwóch równoczesnych żądań łapie unikalny `orders.cart_id` (`uq_orders_cart_id`, `saveAndFlush` + tłumaczenie `DataIntegrityViolationException` w adapterze).
    - Skutek: po timeoucie order-service sam ponawia checkout; gdy i to zawiedzie (503), klient klika „Złóż zamówienie” jeszcze raz i zamówienie powstaje z zamkniętego koszyka.
    - Świadomie bez Kafki w cart-service (cart nie musi wiedzieć, czy zamówienie powstało — strażnikiem „jedno zamówienie na koszyk” jest order-service). Koszyk zamknięty bez zamówienia zostaje zamrożony do czasu ponowienia.
28. **Testcontainers przy działającym całym `docker compose`**: Docker Desktop z 3,8 GB pamięci + 9 kontenerów aplikacji + Postgres i Kafka z testów → testy order-service trwały ~8 min (pojedyncze konteksty po 2–4 min, `KafkaAdmin` „Could not configure topics” przez timeout). Testy przechodzą. Przed `mvnw install` warto zatrzymać compose (`docker compose stop`) albo zwiększyć pamięć Dockera.
19. **Jackson 3 w kodzie produkcyjnym**: wstrzykiwać `tools.jackson.databind.json.JsonMapper`, wyjątki `tools.jackson.core.JacksonException`.
20. **Kafka na Boot 4**: starter **`spring-boot-starter-kafka`** (moduł `spring-boot-kafka`). `@ServiceConnection` działa z `KafkaContainer`.
21. **Testcontainers z kilkoma kontekstami Springa** (różne `@MockitoBean`/`properties` = osobne konteksty) uruchamiały kilka Kafek naraz → jedna nie zdążyła w 60 s („Timed out waiting for log output matching … RECOVERY to RUNNING”). Fix: **statyczne kontenery** startowane raz w `TestcontainersConfiguration` (blok `static`) + `withStartupTimeout(3 min)`; beany `@ServiceConnection` zwracają te same instancje.
22. **Surefire na końcu testów order-service**: „Surefire is going to kill self fork JVM … 30 seconds after System.exit(0)” — producent Kafki zamyka się po zatrzymaniu kontenerów. Testy przechodzą, wynik builda 0; kosmetyka (+30 s).
23. **Kafka w compose bez wolumenu**: odtworzenie kontenera (np. zmiana konfiguracji + `compose up`) kasuje topiki, a przy `auto.create.topics.enable=false` aplikacje tworzą je tylko przy starcie → publisher dostaje „Topic order-placed not present in metadata”. Fix: wolumen `kafka-data:/var/lib/kafka/data` + `KAFKA_LOG_DIRS` + stały `CLUSTER_ID` (katalog w obrazie należy do `appuser`, więc nazwany wolumen ma dobre uprawnienia). Po utracie topiku: restart order-service i invoice-service odtwarza topiki, outbox dośle zaległe zdarzenia.
24. **Healthcheck Kafki przez `kafka-broker-api-versions.sh`** startuje JVM co 10 s; pod obciążeniem (build Mavena w Dockerze + 9 kontenerów na 3,8 GB) przekraczał timeout → Kafka `unhealthy` → `compose up` czekał w nieskończoność. Fix: `nc -z localhost 9092`.
25. **curl z Git Bash a polskie znaki**: treść z „ą/ł” wysyłana w cp1250 → 400 „Malformed request body”. To nie błąd aplikacji — w testach z konsoli używać ASCII albo Bruno.
26. **PDF i polskie znaki**: standardowe fonty PDF (Helvetica itd.) nie mają „ąęłńóśźż” → font TTF (DejaVu Sans) w `resources/fonts` i `useFont(...)` w `PdfRendererBuilder`. Szablon musi być poprawnym XHTML (zamknięte tagi, `&#160;` zamiast `&nbsp;`). Ostrzeżenia PDFBox „Using fallback font LiberationSans for base font …” wyciszone (`logging.level.org.apache.pdfbox.pdmodel.font.PDType1Font=ERROR`).
27. **Thymeleaf CSS**: `table.lines th` ma wyższą specyficzność niż `th.amount` — wyrównanie kwot w nagłówkach trzeba pisać jako `table.lines th.amount`.

---

## 6. Next Steps (plan całego projektu)

### Task 4 — ✅ GOTOWY (szczegóły w sekcjach 2 i 4)
Ograniczenie nr 18 rozwiązane 2026-10-20 (idempotentny checkout + jedno zamówienie na koszyk) — czeka na code review i test w Bruno przez użytkownika.

### Task 5 — gateway-service (Interakcja / BFF) — NASTĘPNY
- Proxy, **tylko** endpointy potrzebne do procesu sprzedaży (3 ekrany). Zwraca **chude DTO**. Frontend rozmawia **wyłącznie** z tym serwisem.
- Feign do product/cart/order (+ invoice: pobranie dokumentu po złożeniu zamówienia). Mapowanie ekranów:
  - Ekran 1 (wybór): `GET /shop/products`, `?type=`, `/{id}/configuration`; dodaj do koszyka.
  - Ekran 2 (dane zamówienia): formularz (klient, adres dostawy, checkbox „chcę fakturę” → `billingAddress`, checkbox „na firmę” → `companyName` + `taxIdentificationNumber`, płatność, dostawa).
  - Ekran 3 (podsumowanie): `GET` koszyka; `POST` złóż zamówienie; link do PDF (dokument powstaje asynchronicznie — 404 przez pierwsze sekundy).

### Task 6 — Pełny Docker Compose + prezentacja
- `docker compose up` całości (wszystkie serwisy + Kafka + bazy).
- Kolekcja Bruno/Postman pokazująca cały proces sprzedaży **wyłącznie** przez gateway-service.

### Reguły pracy (ustalone z użytkownikiem)
- **Endpoint po endpoincie**: jeden działa i jest zweryfikowany (test + Bruno/live), dopiero potem następny. W Task 4 użytkownik pozwolił przechodzić od razu do kolejnego kroku i zdać relację na końcu.
- **Niezależność serwisów**: żadnego współdzielenia modułów między serwisami (każdy ma własny model + własne DTO klienta Feign / rekordy zdarzeń).
- **Bez skrótów w nazwach zmiennych** (wyjątek: `id`, `url`; `sku` rozwinięte na `stockKeepingUnit`, NIP na `taxIdentificationNumber`).
- **Najpierw plan/model do zatwierdzenia**, potem kod. Przy decyzjach pytać konkretnie, bez skrótów myślowych.
- Git ogarnia użytkownik ręcznie.
