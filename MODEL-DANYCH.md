# Model danych — online-store-2

> Odtworzone 2026-10-16 na podstawie kodu (oryginalny plik zaginął). Źródłem prawdy jest kod: klasy w modułach `model` i migracje Liquibase w `adapter/src/main/resources/db/changelog`.

## Zasady wspólne
- **Każdy serwis ma własną bazę Postgres i własny model.** Serwisy nie współdzielą modułów; dane z innego serwisu przychodzą przez Feign i są mapowane na klasy `*Details` w modelu odbiorcy.
- **Identyfikatory**: UUID (wyjątek: tabele add-onów w koszyku/zamówieniu mają techniczny klucz `BIGINT`).
- **`Money`**: value object `(amount: BigDecimal, currency: String)`, w bazie dwie kolumny `<pole>_amount numeric(19,2)` + `<pole>_currency varchar(3)` (`MoneyEmbeddable` w adapterze). Waluta na razie zawsze `PLN`.
- **Snapshot cen**: koszyk kopiuje cenę, nazwę i SKU z katalogu w chwili dodania; zamówienie kopiuje pozycje koszyka w chwili złożenia. Późniejsze zmiany w katalogu nie zmieniają koszyka ani zamówienia.
- **Ceny netto/brutto**: odłożone do Task 4 (faktury). Obecne kwoty to ceny brutto dla klienta.

## product-service (baza `product_service`, port 5432)

| Klasa | Pola |
|---|---|
| `Product` | `id`, `name`, `type: ProductType`, `description`, `basePrice: Money`, `attributes: Map<String,String>`, `options[]`, `variants[]`, `addOns[]` |
| `ProductOption` | `name` (np. „Procesor”), `position`, `values[]` (np. „i5”, „i7”) — wymiar wyboru |
| `ProductVariant` | `id`, `stockKeepingUnit`, `optionValues: Map<opcja, wartość>`, `price: Money`, `availability: Availability` — konkretna kombinacja opcji |
| `AddOn` | `productId` (inny istniejący produkt), `name`, `price: Money` — akcesorium dokupowane do produktu |

Enumy: `ProductType (COMPUTER | SMARTPHONE | ELECTRONICS)`, `Availability (AVAILABLE | SOLD_OUT | COMING_SOON)`.
Tabele: `products`, `product_attributes`, `product_options`, `product_option_values`, `product_variants`, `product_variant_option_values`, `add_ons`.

Decyzje: model wzorowany na Shopify (options/variants); brak stanu magazynowego (osobna domena), tylko `availability`; archetyp „Catalog” świadomie niewydzielony.

## cart-service (baza `cart_service`, port 5433)

| Klasa | Pola |
|---|---|
| `Cart` | `id`, `status: CartStatus`, `items[]`, `total: Money`, `createdAt`, `updatedAt` |
| `CartItem` | `id`, `productId`, `variantId`, `productName`, `stockKeepingUnit`, `unitPrice: Money`, `quantity`, `addOns[]`, `lineTotal: Money` |
| `CartItemAddOn` | `productId`, `name`, `price: Money`, `quantity` |

Enum: `CartStatus (ACTIVE | CHECKED_OUT)`.
Tabele: `carts`, `cart_items`, `cart_item_addons`.

Reguły:
- `lineTotal = unitPrice × quantity + Σ(cena add-onu × ilość add-onu)` — add-on ma własną ilość („2 laptopy, 1 kabel”).
- Ta sama konfiguracja (wariant + zbiór add-onów) dodana ponownie → sumowanie ilości zamiast nowej pozycji.
- Zmieniać można tylko koszyk `ACTIVE`. `checkout` (pusty koszyk → 409) ustawia `CHECKED_OUT` i koszyk staje się niezmienny.

## order-service (baza `order_service`, port 5434)

| Klasa | Pola |
|---|---|
| `Order` | `id`, `orderNumber` (np. `ORD-2026-000001`), `cartId`, `status: OrderStatus`, `customer`, `shippingAddress`, `billingAddress`, `paymentMethod`, `deliveryMethod`, `lines[]`, `itemsTotal`, `deliveryCost`, `grandTotal`, `createdAt` |
| `OrderLine` | `id`, `productId`, `variantId`, `productName`, `stockKeepingUnit`, `unitPrice`, `quantity`, `addOns[]`, `lineTotal` — snapshot pozycji koszyka |
| `OrderLineAddOn` | `productId`, `name`, `price`, `quantity` |
| `Customer` | `firstName`, `lastName`, `email`, `phone` |
| `Address` | `street`, `city`, `postalCode`, `country` |

Enumy:
- `OrderStatus (NEW | PAID | PROCESSING | SHIPPED | COMPLETED | CANCELLED)` — na razie używany tylko `NEW`.
- `PaymentMethod (CARD | BLIK | TRANSFER | CASH_ON_DELIVERY)` — tylko zapisywana, bez obsługi płatności.
- `DeliveryMethod (COURIER 19,99 zł | PARCEL_LOCKER 12,99 zł | PICKUP 0 zł)` — koszt w enumie.

Tabele: `orders` (klient i oba adresy jako kolumny `customer_*`, `shipping_*`, `billing_*`; indeks na `customer_email`), `order_lines` (kolejność w `line_number`), `order_line_addons`, sekwencja `order_number_seq`.

Reguły:
- `itemsTotal = Σ lineTotal`, `grandTotal = itemsTotal + deliveryCost`.
- `billingAddress` opcjonalny: podany = klient chce faktury; brak = zamówienie bez faktury (pole `null`, adres dostawy NIE jest kopiowany).
- Numer zamówienia: `ORD-<rok>-<6 cyfr z sekwencji>`; sekwencja globalna (nie zeruje się co rok), UUID pozostaje identyfikatorem technicznym.
- Klient nie ma konta — historia zamówień po adresie email (`GET /orders?email=`, bez rozróżniania wielkości liter, od najnowszych).

## Przepływ między serwisami
```
cart-service ──Feign GET /products/{id}──▶ product-service      (dodanie do koszyka: snapshot ceny/nazwy/SKU)
order-service ──Feign POST /carts/{id}/checkout──▶ cart-service (złożenie zamówienia: zamknięcie koszyka + jego zawartość)
```
