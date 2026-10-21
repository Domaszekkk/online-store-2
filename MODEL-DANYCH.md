# Model danych — online-store-2

> Odtworzone 2026-10-16 na podstawie kodu (oryginalny plik zaginął), uzupełnione o Task 4 (VAT, faktury) tego samego dnia. Źródłem prawdy jest kod: klasy w modułach `model` i migracje Liquibase w `adapter/src/main/resources/db/changelog`.

## Zasady wspólne
- **Każdy serwis ma własną bazę Postgres i własny model.** Serwisy nie współdzielą modułów; dane z innego serwisu przychodzą przez Feign albo Kafkę i są mapowane na własne klasy odbiorcy (`*Details`, `IssueDocumentCommand`).
- **Identyfikatory**: UUID (wyjątek: tabele add-onów, pozycji dokumentu i podsumowań VAT mają techniczny klucz `BIGINT`).
- **`Money`**: value object `(amount: BigDecimal, currency: String)`, w bazie dwie kolumny `<pole>_amount numeric(19,2)` + `<pole>_currency varchar(3)` (`MoneyEmbeddable` w adapterze). Waluta zawsze `PLN`. W invoice-service kwoty są w kolumnach `numeric(19,2)`, a waluta raz na dokument (`documents.currency`).
- **Snapshot cen**: koszyk kopiuje cenę, stawkę VAT, nazwę i SKU z katalogu w chwili dodania; zamówienie kopiuje pozycje koszyka w chwili złożenia; dokument kopiuje dane zamówienia i sprzedawcy w chwili wystawienia. Późniejsze zmiany nie zmieniają wcześniejszych etapów.
- **Ceny są brutto** (to, co płaci klient). Każda cena ma obok stawkę VAT `VatRate (VAT_23 | VAT_8 | VAT_5 | VAT_0)`. Netto i kwotę VAT liczy dopiero invoice-service.

## product-service (baza `product_service`, port 5432)

| Klasa | Pola |
|---|---|
| `Product` | `id`, `name`, `type: ProductType`, `description`, `basePrice: Money`, `attributes: Map<String,String>`, `options[]`, `variants[]`, `addOns[]` |
| `ProductOption` | `name` (np. „Procesor”), `position`, `values[]` (np. „i5”, „i7”) — wymiar wyboru |
| `ProductVariant` | `id`, `stockKeepingUnit`, `optionValues: Map<opcja, wartość>`, `price: Money` (brutto), `availability: Availability`, `vatRate: VatRate` — konkretna kombinacja opcji |
| `AddOn` | `productId` (inny istniejący produkt), `name`, `price: Money` (brutto), `vatRate: VatRate` — akcesorium dokupowane do produktu |

Enumy: `ProductType (COMPUTER | SMARTPHONE | ELECTRONICS)`, `Availability (AVAILABLE | SOLD_OUT | COMING_SOON)`, `VatRate`.
Tabele: `products`, `product_attributes`, `product_options`, `product_option_values`, `product_variants` (+ `vat_rate`), `product_variant_option_values`, `add_ons` (+ `vat_rate`).

Decyzje: model wzorowany na Shopify (options/variants); brak stanu magazynowego (osobna domena), tylko `availability`; archetyp „Catalog” świadomie niewydzielony; `vatRate` w żądaniach wymagane (bez domyślnej wartości, bo `PUT` zastępuje całą listę wariantów).

## cart-service (baza `cart_service`, port 5433)

| Klasa | Pola |
|---|---|
| `Cart` | `id`, `status: CartStatus`, `items[]`, `total: Money`, `createdAt`, `updatedAt` |
| `CartItem` | `id`, `productId`, `variantId`, `productName`, `stockKeepingUnit`, `unitPrice: Money`, `vatRate`, `quantity`, `addOns[]`, `lineTotal: Money` |
| `CartItemAddOn` | `productId`, `name`, `price: Money`, `vatRate`, `quantity` |

Enum: `CartStatus (ACTIVE | CHECKED_OUT)`.
Tabele: `carts`, `cart_items` (+ `vat_rate`), `cart_item_addons` (+ `vat_rate`).

Reguły:
- `lineTotal = unitPrice × quantity + Σ(cena add-onu × ilość add-onu)` — add-on ma własną ilość („2 laptopy, 1 kabel”).
- Ta sama konfiguracja (wariant + zbiór add-onów) dodana ponownie → sumowanie ilości zamiast nowej pozycji.
- Zmieniać można tylko koszyk `ACTIVE`. `checkout` (pusty koszyk → 409) ustawia `CHECKED_OUT` i koszyk staje się niezmienny.
- `checkout` jest idempotentny: dla koszyka już `CHECKED_OUT` zwraca tę samą zawartość (nic nie zapisuje), żeby order-service i klient mogli bezpiecznie ponowić złożenie zamówienia.

## order-service (baza `order_service`, port 5434)

| Klasa | Pola |
|---|---|
| `Order` | `id`, `orderNumber` (np. `ORD-2026-000001`), `cartId`, `status: OrderStatus`, `customer`, `shippingAddress: Address`, `billingAddress: BillingAddress`, `paymentMethod`, `deliveryMethod`, `lines[]`, `itemsTotal`, `deliveryCost`, `grandTotal`, `createdAt` |
| `OrderLine` | `id`, `productId`, `variantId`, `productName`, `stockKeepingUnit`, `unitPrice`, `vatRate`, `quantity`, `addOns[]`, `lineTotal` — snapshot pozycji koszyka |
| `OrderLineAddOn` | `productId`, `name`, `price`, `vatRate`, `quantity` |
| `Customer` | `firstName`, `lastName`, `email`, `phone` |
| `Address` | `street`, `city`, `postalCode`, `country` (adres dostawy) |
| `BillingAddress` | `companyName`, `taxIdentificationNumber` (NIP), `street`, `city`, `postalCode`, `country` |

Enumy:
- `OrderStatus (NEW | PAID | PROCESSING | SHIPPED | COMPLETED | CANCELLED)` — na razie używany tylko `NEW`.
- `PaymentMethod (CARD | BLIK | TRANSFER | CASH_ON_DELIVERY)` — tylko zapisywana, bez obsługi płatności.
- `DeliveryMethod (COURIER 19,99 zł | PARCEL_LOCKER 12,99 zł | PICKUP 0 zł)` — koszt w enumie, VAT dostawy zawsze `VAT_23`.

Tabele: `orders` (klient i oba adresy jako kolumny `customer_*`, `shipping_*`, `billing_*` + `billing_company_name`, `billing_tax_identification_number`; indeks na `customer_email`), `order_lines` (+ `vat_rate`, kolejność w `line_number`), `order_line_addons` (+ `vat_rate`), sekwencja `order_number_seq`, `outbox_events` (`id`, `aggregate_id`, `event_type`, `payload` TEXT z JSON, `created_at`, `published_at`; indeks częściowy na niewysłanych).

Reguły:
- `itemsTotal = Σ lineTotal`, `grandTotal = itemsTotal + deliveryCost`.
- Jedno zamówienie na koszyk: unikalne `orders.cart_id` (`uq_orders_cart_id`); drugie złożenie z tego samego koszyka → 409 z numerem pierwszego zamówienia.
- `billingAddress` opcjonalny: podany = klient chce faktury; brak = potwierdzenie zakupu zamiast faktury (pole `null`, adres dostawy NIE jest kopiowany).
- Faktura na firmę albo na osobę wynika z danych: `companyName` + `taxIdentificationNumber` podane razem = firma; oba puste = osoba prywatna; jedno bez drugiego → 400. NIP: 10 cyfr + suma kontrolna (wagi 6,5,7,2,3,4,5,6,7, reszta z dzielenia przez 11). Walidacja w `public` (adnotacje) i w modelu (`BillingAddress.requireValid()`, wywoływane PRZED zamknięciem koszyka).
- Numer zamówienia: `ORD-<rok>-<6 cyfr z sekwencji>`; sekwencja globalna (nie zeruje się co rok), UUID pozostaje identyfikatorem technicznym.
- Klient nie ma konta — historia zamówień po adresie email (`GET /orders?email=`, bez rozróżniania wielkości liter, od najnowszych).
- Zapis zamówienia i wpis `OrderPlaced` do `outbox_events` są w jednej transakcji; publisher co sekundę wysyła niewysłane wpisy na topic `order-placed` (klucz = `orderId`, nagłówki `eventType`, `eventId`) i ustawia `published_at`.

## invoice-service (baza `invoice_service`, port 5435)

| Klasa | Pola |
|---|---|
| `Document` | `id`, `type: DocumentType`, `number`, `orderId`, `orderNumber`, `issuedAt`, `saleDate` (= chwila złożenia zamówienia), `paymentMethod`, `deliveryMethod`, `seller: Seller`, `buyer: Buyer` (`null` dla potwierdzenia zakupu), `lines[]`, `vatSummaries[]`, `netTotal`, `vatTotal`, `grossTotal` |
| `DocumentLine` | `position`, `name`, `stockKeepingUnit`, `quantity`, `unitGrossPrice`, `vatRate`, `netTotal`, `vatTotal`, `grossTotal` — produkt, każdy add-on i dostawa (gdy koszt > 0) to osobne pozycje |
| `VatSummary` | `vatRate`, `netTotal`, `vatTotal`, `grossTotal` — jedna na stawkę |
| `Seller` | `name`, `taxIdentificationNumber`, `street`, `city`, `postalCode`, `country`, `bankAccountNumber` — z `application.properties` (`invoice.seller.*`), kopiowany do każdego dokumentu |
| `Buyer` | `fullName`, `companyName`, `taxIdentificationNumber`, `street`, `city`, `postalCode`, `country`, `email` |

Enum: `DocumentType (INVOICE „FV” | PURCHASE_CONFIRMATION „PZ”)`.
Tabele: `documents` (seller i buyer jako kolumny `seller_*`, `buyer_*`; unikalne `number` i `order_id`), `document_lines` (kolejność w `line_number`), `document_vat_summaries` (kolejność w `summary_order`), `document_pdfs` (`document_id`, `order_id`, `content` BYTEA), `document_number_counters` (`document_type`, `period` `YYYY-MM`, `last_number`; klucz złożony).

Reguły:
- Typ: zamówienie z `billingAddress` → `INVOICE`, bez → `PURCHASE_CONFIRMATION`.
- Numer: `FV/<rok>/<miesiąc>/<6 cyfr>` i `PZ/<rok>/<miesiąc>/<6 cyfr>`; osobny licznik na typ i miesiąc (wg daty wystawienia w strefie Europe/Warsaw), nadawany jednym `INSERT … ON CONFLICT DO UPDATE … RETURNING` w tej samej transakcji co zapis dokumentu (brak dziur przy błędzie).
- VAT od brutto: netto pozycji = brutto × 100 / (100 + stawka), zaokrąglenie HALF_UP do groszy, VAT = brutto − netto. Podsumowanie liczone od sumy brutto w każdej stawce (to ono jest wiążące); sumy dokumentu = suma podsumowań.
- PDF generowany raz przy wystawieniu (Thymeleaf `templates/document.html` → OpenHTMLToPDF, font DejaVu Sans z polskimi znakami) i zapisany w `document_pdfs`.
- Idempotencja: dokument dla `orderId` już istnieje → zdarzenie jest pomijane (plus unikalny `order_id` w bazie).

## Przepływ między serwisami
```
cart-service ──Feign GET /products/{id}──▶ product-service       (dodanie do koszyka: snapshot ceny/VAT/nazwy/SKU)
order-service ──Feign POST /carts/{id}/checkout──▶ cart-service  (złożenie zamówienia: zamknięcie koszyka + jego zawartość)
order-service ──outbox → Kafka topic order-placed──▶ invoice-service (OrderPlaced → faktura albo potwierdzenie zakupu)
invoice-service ──błędna wiadomość──▶ topic order-placed-dlt
```
