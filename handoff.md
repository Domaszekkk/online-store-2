# Handoff — Mikroserwisowy System Sklepu Internetowego

> Monorepo: `online-store-2`. Java 21, Spring Boot 4.1.1, architektura heksagonalna, Docker, Postgres, Liquibase, Feign, Swagger (Kafka dojdzie w Task 4).
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

Każdy serwis: **Maven multi-module** (`model → domain → public → adapter`), **własna baza Postgres**, Liquibase, Swagger, MapStruct, testy (unit / web-slice / integracja Testcontainers). Komunikacja między serwisami przez **Feign**. Snapshot cen (koszyk/zamówienie pamiętają cenę z chwili dodania). UUID jako ID. `Money` jako value object.

Pełny model danych i uzasadnienie: `MODEL-DANYCH.md`.

---

## 2. Current State

**✅ Task 1 — product-service — GOTOWY** (port `8080`, baza `5432`).
Endpointy: `POST /products`, `GET /products`, `GET /products?type=`, `GET /products/{id}`, `GET /products/{id}/configuration`, `PUT /products/{id}` (pełny: także options/variants/addOns), `DELETE /products/{id}`.
Testy: domena 16/16, kontroler (web-slice) 12/12, integracja (Testcontainers) 9/9.

**✅ Task 2 — cart-service — GOTOWY** (port `8081`, baza `5433`).
Endpointy: `POST /carts`, `GET /carts/{id}`, `POST /carts/{id}/items` (Feign → product-service: snapshot ceny/SKU/nazwy, sprawdzanie dostępności; ta sama konfiguracja = zwiększenie ilości), `PATCH /carts/{id}/items/{itemId}` (zmiana ilości), `DELETE /carts/{id}/items/{itemId}`.
Testy: domena 19/19, kontroler 12/12, integracja 6/6, retry Feign (stub HTTP z JDK) 3/3.

**Oba serwisy działają razem** przez `docker compose up --build` (zweryfikowane end-to-end 2026-10-15: produkt → koszyk → dodanie ×2 (scalenie) → PATCH → DELETE; 2 laptopy + 1 kabel = 9237.00; błędne body → 400 z listą pól; awaria product-service → 503 po 3 próbach). Kolekcje Bruno: `product-service/bruno` (01 akcesorium → 02 laptop z add-onem → … 08) i `cart-service/bruno` (01–05).

**✅ Poprawki po review (2026-10-15)** — patrz sekcja 4.

**⏳ Task 3–6 — do zrobienia.**

Git: zarządza użytkownik ręcznie (nic nie commitowane automatycznie).

---

## 3. Active Files

```
online-store-2/
├─ docker-compose.yml            # product-service(+ -db) + cart-service(+ -db); cart czeka na healthy product
├─ MODEL-DANYCH.md               # specyfikacja modelu danych — UWAGA: pliku NIE MA w repo (stan 2026-10-15), do odtworzenia
├─ handoff.md                    # ten plik
├─ product-service/              # Task 1 (artifactId: product-service; moduły: product-model/-domain/-public/-adapter)
│  ├─ pom.xml, Dockerfile, .mvn/ (wrapper 3.9.15), bruno/
│  ├─ model/   .../com/estore/product/   Product, ProductOption, ProductVariant, AddOn, Money,
│  │                                     ProductType, Availability, AddProductCommand, UpdateProductCommand,
│  │                                     ProductException, ProductNotFoundException, InvalidAddOnException
│  ├─ domain/  ProductService, ProductRepositoryPort
│  ├─ public/  (zależy od product-model — enumy w żądaniach)
│  │           odpowiedzi: ProductDto, ProductOptionDto, ProductVariantDto, AddOnDto, MoneyDto, ErrorMessage
│  │           żądania (Bean Validation): AddProductRequest, UpdateProductRequest, ProductOptionRequest,
│  │                                       ProductVariantRequest, AddOnRequest, MoneyRequest
│  └─ adapter/ ProductController, *Entity, ProductJpaRepository, ProductRepositoryAdapter,
│              ProductMapper (MapStruct), ProductServiceConfig, GlobalExceptionHandler, OpenApiConfig,
│              resources/application.properties, db/changelog/** (Liquibase)
└─ cart-service/                 # Task 2 (artifactId: cart-service; moduły: cart-model/-domain/-public/-adapter)
   ├─ pom.xml, Dockerfile, .mvn/ (wrapper 3.9.15), bruno/
   ├─ model/   .../com/estore/cart/   Cart, CartItem, CartItemAddOn (+quantity), Money (+add/multiply),
   │                                  CartStatus, AddItemCommand, AddOnSelection, ChangeItemQuantityCommand,
   │                                  ProductDetails, VariantDetails, AddOnDetails,
   │                                  CartException, CartNotFoundException, ProductNotFoundException,
   │                                  VariantNotFoundException, VariantNotAvailableException,
   │                                  CartNotActiveException, CartItemNotFoundException,
   │                                  InvalidQuantityException, ProductCatalogUnavailableException
   ├─ domain/  CartService, CartRepositoryPort, ProductCatalogPort
   ├─ public/  odpowiedzi: CartDto, CartItemDto, CartItemAddOnDto (+quantity), MoneyDto, ErrorMessage
   │           żądania (Bean Validation): AddItemRequest, AddOnSelectionRequest, ChangeItemQuantityRequest
   └─ adapter/ CartController, *Entity, CartJpaRepository, CartRepositoryAdapter, CartMapper,
               CartServiceConfig, GlobalExceptionHandler, OpenApiConfig,
               client/ ProductClient (@FeignClient), ProductResponse/VariantResponse/AddOnResponse/MoneyPayload (records),
                       ProductCatalogAdapter (implements ProductCatalogPort),
                       ProductClientConfiguration (Retryer + ErrorDecoder, tylko dla ProductClient),
                       RetryOnServerErrorDecoder (5xx → RetryableException)
               resources/application.properties (product.service.url, timeouty Feign, product.service.retry.*), db/changelog/**
```

Kluczowe stałe wersje (w pomach): Spring Boot **4.1.1**, springdoc **3.1.1**, MapStruct **1.6.3**, lombok-mapstruct-binding **0.2.0**, Testcontainers BOM **2.0.5**, Spring Cloud **2025.1.3** (OpenFeign 5.x), Postgres **17**, Maven wrapper **3.9.15**.

---

## 4. Changes Made

- **product-service**: pełny heksagon, generyczny model produktu (options = wymiary wyboru, variants = konkretne kombinacje z ceną/SKU/stanem, addOns = akcesoria), `Availability` enum (AVAILABLE / SOLD_OUT / COMING_SOON) z metodą `isAvailable()` — **brak `stock`** (stan magazynowy to osobna domena). Tabele relacyjne + Liquibase. 6 endpointów + Swagger. Utwardzony `Product.from` (null-safe kolekcje).
- **cart-service**: pełny heksagon, koszyk z pozycjami, snapshot cen, liczenie `lineTotal`/`total` (Money z `add`/`multiply`). **Feign** `ProductClient` → product-service; `ProductCatalogPort` (port) + `ProductCatalogAdapter` (adapter) mapują odpowiedź na model domeny. Walidacja: wariant nieznaleziony (404), niedostępny (409). DTO odpowiedzi Feign jako **rekordy** (ProductResponse itd.).
- **Nazewnictwo modułów**: unikalne `artifactId` per serwis (`product-model`/`product-adapter`, `cart-model`/`cart-adapter`) — patrz sekcja 5.
- **docker-compose.yml**: 2 serwisy + 2 bazy, `cart-service` ma `PRODUCT_SERVICE_URL=http://product-service:8080` i `depends_on product-service`.
- **Bruno**: kolekcje `product-service/bruno` (01–08) i `cart-service/bruno` (01–05).

### Poprawki po review (2026-10-15)
- **cart — reguły koszyka (w modelu `Cart`/`CartItem`)**: modyfikacja tylko gdy `status == ACTIVE` (inaczej 409 `CartNotActiveException`); ta sama konfiguracja (wariant + zbiór add-onów) → suma ilości pozycji **i** add-onów zamiast nowej pozycji; walidacja ilości (`InvalidQuantityException`, 400) w `CartItem`.
- **cart — add-on ma własną ilość** (decyzja użytkownika: „2 laptopy, 1 kabel”): żądanie `"addOns":[{"productId":…,"quantity":1}]`; `lineTotal = unitPrice × quantity + Σ(cena add-onu × ilość add-onu)`; ten sam add-on podany 2× w żądaniu → ilości sumowane; zmiana ilości pozycji (PATCH) nie zmienia ilości add-onów. Liquibase `003-add-addon-quantity` (kolumna `cart_item_addons.quantity`, istniejące wiersze = 1).
- **Bean Validation (oba serwisy)**: `spring-boot-starter-validation` w adapterze, `jakarta.validation-api` w `public`. Klasy żądań z adnotacjami w `public`, kontroler `@Valid` → MapStruct `toCommand(...)` → komenda z `model` (domena bez zależności od frameworka). `GlobalExceptionHandler`: `MethodArgumentNotValidException` → 400 `"Validation failed: pole: komunikat; …"` (posortowane), `HttpMessageNotReadableException` (zły JSON / nieznany enum) → 400. Reguły biznesowe (koszyk aktywny, add-on istnieje, ilość ≥ 1) zostają też w domenie.
- **product — `UpdateProductRequest` (model) → `UpdateProductCommand`**; nazwa `UpdateProductRequest` jest teraz klasą żądania w `public`.
- **product — naprawiony błąd**: `PUT` bez `attributes` kasował atrybuty (`@Builder.Default` w komendzie update → pusta mapa). Usunięty default; test integracyjny pilnuje, że pominięte kolekcje zostają bez zmian.
- **cart — nowe endpointy**: `PATCH /carts/{id}/items/{itemId}` (body `{"quantity":n}`), `DELETE /carts/{id}/items/{itemId}`; oba zwracają zaktualizowany koszyk (200).
- **cart — odporność Feign**: `Retryer.Default(100ms, 1s, 3 próby)`, connect-timeout 2s / read-timeout 3s, `RetryOnServerErrorDecoder` (5xx też ponawiane; 404 nigdy), po wyczerpaniu prób `ProductCatalogUnavailableException` → **503** (wcześniej 500). Konfiguracja tylko dla `ProductClient` (`configuration = ProductClientConfiguration.class`, klasa celowo bez `@Configuration`).
- **product — pełny `PUT`**: żądanie/komenda update ma `options`/`variants`/`addOns`; `null` = bez zmian, podana lista (także pusta) = zastąpienie. Warianty z istniejącym `id` zachowują je (referencje z koszyków/zamówień pozostają ważne), nowe dostają UUID.
- **product — walidacja add-onów**: `productId` wymagany, musi istnieć w katalogu, nie może wskazywać na siebie → 400 `InvalidAddOnException`.
- **Build**: `mapstruct-processor` + `lombok-mapstruct-binding` przeniesione do `maven-compiler-plugin/annotationProcessorPaths` (nie trafiają do jara).
- **Docker**: `RUN --mount=type=cache,target=/root/.m2` (cache zależności między buildami), `HEALTHCHECK` przez `curl /actuator/health`; w compose baza produktu przemianowana `postgres-db` → `product-service-db` (wolumen bez zmian), cart `depends_on product-service: service_healthy`.
- **Actuator**: cart eksponuje też `metrics` (spójnie z product).
- **Waluta**: świadomie zostaje sztywne `PLN` (decyzja użytkownika).

---

## 5. Failed Attempts / Gotchas (Spring Boot 4 — WAŻNE, nie powtarzać)

Boot 4 mocno zmodularyzował i przeszedł na Jackson 3 — to kosztowało serię poprawek. Dla kolejnych serwisów skopiować pom cart-service jako wzorzec.

1. **Maven wrapper 3.10.0 → IntelliJ „Unable to create injector" (Guice).** Fix: przypiąć wrapper do **3.9.15** w `.mvn/wrapper/maven-wrapper.properties` (CLI działa z 3.10, IntelliJ nie).
2. **Testcontainers**: Boot 4 nie zarządza ich wersjami, a w **2.0** zmieniono artefakty na prefiks. Fix: import `testcontainers-bom:2.0.5` + artefakty `testcontainers-junit-jupiter`, `testcontainers-postgresql` (nie `junit-jupiter`/`postgresql`).
3. **`@AutoConfigureMockMvc` / `@WebMvcTest`** przeniesione do `org.springframework.boot.webmvc.test.autoconfigure` i wymagają zależności **`spring-boot-starter-webmvc-test`** (test scope).
4. **Liquibase** nie startuje z samym `liquibase-core` — trzeba modułu **`spring-boot-liquibase`** (inaczej cicho nie tworzy tabel → Hibernate `validate` krzyczy „missing table").
5. **Jackson 3** (`tools.jackson`) jest domyślny → nie ma beana `com.fasterxml.jackson...ObjectMapper`. W testach używać lokalnego `new ObjectMapper()`.
6. **MapStruct + Lombok builder**: pole `addOns` jest interpretowane jako adder (`add`+`Ons`) → „Unknown property addOns". Fix: `@Mapper(componentModel="spring", builder=@Builder(disableBuilder=true))` (mapowanie przez settery).
7. **`@Builder.Default` + `@NoArgsConstructor`**: Jackson deserializuje bez pola → kolekcja `null` (nie pusta) → NPE. Fix: null-guard w `from(...)` / serwisie (nie polegać na defaultcie buildera przy deserializacji JSON).
8. **Identyczne `artifactId` między serwisami** (`com.estore:model` w obu) → IntelliJ dopiski `(1)` i kolizja w `~/.m2`. Fix: **unikalne artifactId per serwis** (`<service>-model` itd.), foldery/pakiety bez zmian.
9. **Unidirectional `@OneToMany @JoinColumn`**: Hibernate wstawia dziecko bez FK, potem UPDATE → kolumna FK **musi być nullable** w Liquibase (nie dawać `nullable: false` na `*_id` FK).
10. **BigDecimal w asercjach**: `equals` jest czułe na skalę (`3999.00` ≠ `3999.0`) → porównywać `compareTo(...) == 0`.
11. **Feign na Boot 4**: działa przez **Spring Cloud 2025.1.3** (OpenFeign 5.x). Import `spring-cloud-dependencies:2025.1.3` BOM + `spring-cloud-starter-openfeign`; `@EnableFeignClients` na klasie `*Application`.
12. **Feign `Retryer` sam z siebie NIE ponawia 5xx** — ponawia tylko `RetryableException` (błędy I/O, timeouty, nagłówek `Retry-After`). Żeby ponawiać 503/500 trzeba własnego `ErrorDecoder` (u nas `RetryOnServerErrorDecoder`). Konfiguracja klienta jako klasa **bez** `@Configuration`, wskazana w `@FeignClient(configuration=...)` — inaczej staje się globalna dla wszystkich klientów.
13. **`annotationProcessorPaths`**: gdy się je ustawia, Maven przestaje wykrywać procesory z classpath → **Lombok też musi być na liście** (`${lombok.version}` z BOM Boota), inaczej brak getterów/builderów w kompilacji.
14. **Kolejność w Bruno/testach**: add-on musi wskazywać istniejący produkt → najpierw tworzyć akcesorium, potem produkt główny (kolekcja product: 01 ELECTRONICS → 02 COMPUTER).
15. **`@Builder.Default` a konstruktor bezargumentowy (zaobserwowane 2026-10-15)**: MapStruct (setter mapping) tworzy obiekt przez `@NoArgsConstructor`, a ten **inicjalizuje** pola `@Builder.Default` (pusta mapa/lista), a MapStruct nie nadpisuje ich, gdy źródło jest `null`. W komendach typu *update*, gdzie `null` znaczy „bez zmian”, **nie dawać `@Builder.Default`** — inaczej pominięte pole czyści dane.
16. **Bean Validation w heksagonie**: adnotacje `jakarta.validation` tylko na klasach żądań w `public`, nie na komendach w `model`. Nazwy klas żądań nie mogą kolidować z klasami `model` (ten sam pakiet `com.estore.<service>` w obu modułach).

---

## 6. Next Steps (plan całego projektu)

### Decyzje podjęte 2026-10-15
- **Bean Validation** — ✅ wprowadzone (patrz sekcja 4). Kolejne serwisy (order, gateway): od razu klasy żądań z walidacją w `public`.
- **Add-on z własną ilością** — ✅ wprowadzone.
- **Ceny netto/brutto** — ⏸ ODŁOŻONE do Task 4 (faktury); do tego czasu nie mieszać w modelu cen. Notatka na później: cena + stawka VAT (`VatRate`) na wariancie, API zwraca `net`/`gross`/`vatRate`, koszyk/zamówienie snapshotują oba i sumują `netTotal`/`vatTotal`/`grossTotal`; do ustalenia źródło prawdy (rekomendacja: brutto).
- **Archetyp „Catalog”** — ❌ zostajemy przy obecnym modelu (product-service świadomie łączy Product + Catalog; wydzielenie miałoby sens przy wielu cennikach/kanałach/ofertach czasowych).
- **`MODEL-DANYCH.md`** — brak w repo; odtworzyć (użytkownik ma sprawdzić, czy ma go lokalnie).

### Task 3 — order-service (Zamówienia) — NASTĘPNY
Nowy mikroserwis tym samym wzorcem (skopiować pom cart-service). Port `8082`, baza `order-service-db` (`5434`).
- **Model**: `Order` (orderNumber, status, customer, shippingAddress, billingAddress?, paymentMethod, deliveryMethod, lines[], itemsTotal, deliveryCost, grandTotal, createdAt), `OrderLine` (snapshot: productId, variantId, nameSnapshot, unitPrice, quantity, lineTotal), `Customer` (firstName, lastName, email, phone), `Address` (street, city, postalCode, country), `Money`. Enumy: `OrderStatus (NEW|PAID|PROCESSING|SHIPPED|COMPLETED|CANCELLED)`, `PaymentMethod (CARD|BLIK|TRANSFER|CASH_ON_DELIVERY)`, `DeliveryMethod (COURIER|PARCEL_LOCKER|PICKUP)`.
- **Endpointy**: `POST /orders` (złóż — na podstawie `cartId` + danych klienta/dostawy; pobiera koszyk z cart-service przez **Feign**, snapshot pozycji), `GET /orders/{id}`, `GET /orders` (historia).
- **Feign**: order → cart (pobranie zawartości koszyka). Po złożeniu: oznaczyć koszyk jako `CHECKED_OUT` (opcjonalnie).
- Robić **endpoint po endpoincie** (najpierw `POST /orders` + `GET`, potem historia), weryfikować w Bruno.

### Task 4 — Fakturowanie przez Kafkę
- Dodać **Kafka** do compose. order-service publikuje `OrderPlaced` po złożeniu.
- Konsument generuje **fakturę PDF** (rekomendacja: `OpenHTMLToPDF` + szablon HTML, bajty w `bytea`), endpoint `GET /orders/{id}/invoice` (`application/pdf`). Konsument może być w order-service albo osobnym serwisie.

### Task 5 — gateway-service (Interakcja / BFF)
- Proxy, **tylko** endpointy potrzebne do procesu sprzedaży (3 ekrany). Zwraca **chude DTO** (nie przepuszcza nadmiarowych pól). Frontend rozmawia **wyłącznie** z tym serwisem.
- Feign do product/cart/order. Mapowanie ekranów:
  - Ekran 1 (wybór): `GET /shop/products`, `?type=`, `/{id}/configuration`; dodaj do koszyka.
  - Ekran 2 (dane zamówienia): formularz (klient, adres, płatność, dostawa).
  - Ekran 3 (podsumowanie): `GET` koszyka; `POST` złóż zamówienie.

### Task 6 — Pełny Docker Compose + prezentacja
- `docker compose up` całości (wszystkie serwisy + Kafka + bazy).
- Kolekcja Bruno/Postman pokazująca cały proces sprzedaży **wyłącznie** przez gateway-service.

### Reguły pracy (ustalone z użytkownikiem)
- **Endpoint po endpoincie**: jeden działa i jest zweryfikowany (test + Bruno/live), dopiero potem następny. Nie generować wszystkiego naraz.
- **Niezależność serwisów**: żadnego współdzielenia modułów między serwisami (każdy ma własny model + własne DTO klienta Feign).
- **Bez skrótów w nazwach zmiennych** (wyjątek przyjętych terminów: `id`, `sku`→rozwinięte na `stockKeepingUnit`, `url`).
- **Najpierw plan/model do zatwierdzenia**, potem kod. Przy decyzjach pytać (ID type, persystencja, komunikacja itd.).
- Git ogarnia użytkownik ręcznie.
