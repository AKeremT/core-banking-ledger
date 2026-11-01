# 🏦 High-Integrity Core Banking Ledger & Fund Transfer Engine

> 🇬🇧 **[Click here for English Documentation (README.md)](./README.md)**

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-JUnit5-darkblue.svg?style=for-the-badge&logo=testcontainers)](https://testcontainers.com/)
[![Build Status](https://img.shields.io/badge/Tests-8%2F8%20Passing-success.svg?style=for-the-badge&logo=junit5)](https://github.com/AKeremT/core-banking-ledger)
[![Architecture](https://img.shields.io/badge/Architecture-Double--Entry%20Bookkeeping-blueviolet.svg?style=for-the-badge)](https://en.wikipedia.org/wiki/Double-entry_bookkeeping)

---

## 📖 Genel Bakış ve Problem Tanımı

Geleneksel web uygulamalarında bakiye yönetimi genellikle `accounts.balance = accounts.balance - 100` gibi doğrudan veritabanı alanı güncelleme mantığı üzerine kurulur. Ancak kurumsal bankacılık ve finansal defter (ledger) sistemlerinde bu yaklaşım kabul edilemez riskler barındırır:

1. **İzlenemezlik (Audit Failure):** Paranın nereden geldiği, nereye gittiği ve hangi atomik işlemle çıktığı geriye dönük matematiksel olarak ispatlanamaz.
2. **Yarış Durumu ve Çift Harcama (Race Conditions & Double Spending):** Birden fazla kanal (ATM, POS, Mobil Bankacılık) aynı anda aynı bakiyeyi okuyup harcama yapmaya çalışırsa bakiye tutarsızlaşır veya negatife düşer.
3. **Karşılıklı Kilitlenme (Deadlock):** A kullanıcısı B'ye para gönderirken (A kilitlenip B'yi bekler), eşzamanlı olarak B kullanıcısı A'ya para gönderirse (B kilitlenip A'yı bekler) sistem kilitlenir ve işlem havuzu tükenir.
4. **Ağ Güvensizliği ve Mükerrer İstekler (Network Flakiness):** İstemcinin isteği zaman aşımına uğrayıp yeniden denendiğinde (retry mekanizması) aynı işlem mükerrer çalışarak hesabın birden çok kez borçlandırılmasına yol açabilir.

**`core-banking-ledger`**, bu kritik finansal problemleri çözmek üzere inşa edilmiş; **Çift Taraflı Muhasebe (Double-Entry Bookkeeping)** prensiplerini işleten, **Ağ tekrarlarına karşı Idempotent** çalışan, **Deadlock-Free Lock Ordering (Sıralı Kilit)** mimarisiyle yüksek eşzamanlılık (concurrency) güvencesi sunan ve **değişmez (immutable/append-only)** defter kayıtları tutan yeni nesil bir çekirdek bankacılık motorudur.

---

## 🏛️ Temel Mimari Prensipler

```
                                  ╔══════════════════════════════╗
                                  ║    İstemci Transfer İsteği   ║
                                  ╚══════════════╦═══════════════╝
                                                 ▼
               ┌──────────────────────────────────────────────────────────────────┐
               │ 🛡️ 1. GÜVENLİK KAPISI: Idempotency Kalkanı                       │
               │    (Aynı Idempotency-Key ile mükerrer işlem anında önlenir)      │
               └─────────────────────────────────┬────────────────────────────────┘
                                                 ▼
               ┌──────────────────────────────────────────────────────────────────┐
               │ 🔍 2. GÜVENLİK KAPISI: Mantıksal Validasyonlar                  │
               │    (Self-Transfer, Para Birimi Eşleşmesi, Hesap Statü Kontrolü) │
               └─────────────────────────────────┬────────────────────────────────┘
                                                 ▼
               ┌──────────────────────────────────────────────────────────────────┐
               │ 🔒 3. GÜVENLİK KAPISI: Deadlock-Free Lock Ordering               │
               │    (min(IBAN_A, IBAN_B) sıralamasıyla PESSIMISTIC_WRITE kilidi)  │
               └─────────────────────────────────┬────────────────────────────────┘
                                                 ▼
               ┌──────────────────────────────────────────────────────────────────┐
               │ 💰 4. GÜVENLİK KAPISI: Bakiye Yeterlilik Kontrolü                │
               │    (Kilitli ve güncel satırdan bakiye doğrulaması)               │
               └─────────────────────────────────┬────────────────────────────────┘
                                                 ▼
               ┌──────────────────────────────────────────────────────────────────┐
               │ ⚖️ 5. ÇİFT TARAFLI DEFTER KAYDI & BAKİYE GÜNCELLEMESİ            │
               │    (1 Atomik TX: 1 DEBIT + 1 CREDIT -> ∑ DEBIT = ∑ CREDIT)       │
               └──────────────────────────────────────────────────────────────────┘
```

### 1. Çift Taraflı Muhasebe (Double-Entry Bookkeeping)
Sistemde bakiye "büyüyle" yaratılmaz veya silinmez. Dışarıdan para yatırıldığında dahi işlem **Sistem Kasa Hesabı (System Transit/Vault Account)** ile müşteri hesabı arasında yürütülür.
* Gönderen / Kasa Hesabı: **`DEBIT` (Borç)**
* Alıcı / Müşteri Hesabı: **`CREDIT` (Alacak)**
* Matematiksel Finansal Denge: $\sum \text{DEBIT} = \sum \text{CREDIT}$

### 2. Deadlock Önleyici Sıralı Kilitleme (Lock Ordering)
İki hesabın karşılıklı transferlerinde kilitlenme riskini (`Deadlock`) sıfırlamak için kilit sırası gönderen/alıcı rolüne göre değil; hesap numaralarının **doğal alfabetik sırasına (`Natural Ordering`)** göre belirlenir:
```java
boolean isSenderFirst = senderAccountNumber.compareTo(receiverAccountNumber) < 0;
String firstLock  = isSenderFirst ? senderAccountNumber : receiverAccountNumber;
String secondLock = isSenderFirst ? receiverAccountNumber : senderAccountNumber;
```

### 3. İstemci Düzeyinde Idempotency Kalkanı
Her finansal işlem için HTTP Header'ında benzersiz bir `Idempotency-Key` (UUID) zorunludur. İşlem başlamadan önce veritabanı kontrol edilir; mükerrer bir istek gelirse kilit alma ve bakiye güncelleme adımları atlanarak ilk başarılı işlemin cevabı anında dönülür.

### 4. Değişmez Defter Kayıtları (Immutable / Append-Only)
`LedgerEntry` tablosuna yazılan hiçbir finansal satır güncellenemez veya silinemez (`updatable = false`). Hatalı işlemler ancak ters yönde yeni bir düzeltme kaydıyla dengelenebilir.

### 5. Finansal Sayısal Bütünlük (Banker's Precision)
Tüm parasal değerler `BigDecimal` ve `NUMERIC(19, 4)` hassasiyetinde tutulur. Veritabanı seviyesinde tanımlanan `@CheckConstraint` kuralları ile negatif bakiye (`balance >= 0`) ve geçersiz işlem tutarları (`amount > 0`) fiziksel olarak engellenir.

---

## 📊 Veritabanı ve Varlık-İlişki Mimarisi (Entity-Relationship)

```mermaid
erDiagram
    CUSTOMERS ||--o{ ACCOUNTS : "has (1:N)"
    ACCOUNTS ||--o{ LEDGER_ENTRIES : "records (1:N)"
    TRANSACTIONS ||--|{ LEDGER_ENTRIES : "contains (1:N)"

    CUSTOMERS {
        bigint id PK "IDENTITY, Generated"
        varchar identity_number UK "TCKN / Vergi No, NOT NULL"
        varchar first_name "NOT NULL"
        varchar last_name "NOT NULL"
        varchar email UK "NOT NULL"
        timestamptz created_at "Auto Timestamp"
        timestamptz updated_at "Auto Timestamp"
    }

    ACCOUNTS {
        bigint id PK "IDENTITY, Generated"
        bigint customer_id FK "NOT NULL -> customers.id"
        varchar account_number UK "IBAN / Hesap No, NOT NULL, updatable=false"
        bigint version "Optimistic Locking (@Version)"
        varchar currency "Enum: TRY, USD, EUR, NOT NULL, updatable=false"
        numeric balance "precision 19, scale 4, NOT NULL, CHECK(balance >= 0)"
        varchar status "Enum: ACTIVE, SUSPENDED, CLOSED, NOT NULL"
        varchar account_type "Enum: CHECKING, SAVINGS, NOT NULL"
        timestamptz created_at "Auto Timestamp, updatable=false"
        timestamptz updated_at "Auto Timestamp"
    }

    TRANSACTIONS {
        uuid id PK "UUID, Generated"
        varchar idempotency_key UK "NOT NULL"
        varchar referenceNumber UK "TXN/DEP Formatında Dekont No"
        varchar type "Enum: INTERNAL_TRANSFER, DEPOSIT, WITHDRAW, NOT NULL"
        varchar status "Enum: PENDING, COMPLETED, FAILED, NOT NULL"
        varchar description "Açıklama Metni"
        numeric amount "precision 19, scale 4, NOT NULL, CHECK(amount > 0)"
        varchar currency "Enum: TRY, USD, EUR, NOT NULL"
        timestamptz created_at "Auto Timestamp, updatable=false"
    }

    LEDGER_ENTRIES {
        bigint id PK "IDENTITY, Generated"
        bigint account_id FK "NOT NULL -> accounts.id"
        uuid transaction_id FK "NOT NULL -> transactions.id"
        varchar entry_type "Enum: DEBIT, CREDIT, updatable=false, NOT NULL"
        numeric amount "precision 19, scale 4, updatable=false, NOT NULL"
        timestamptz created_at "Auto Timestamp"
    }
```

### Fiziksel Tablo Özeti

| Tablo Adı | Birincil Anahtar | Kritik Kısıtlar (Constraints) | Açıklama |
| :--- | :--- | :--- | :--- |
| `customers` | `BIGINT (IDENTITY)` | `identity_number UNIQUE`, `email UNIQUE` | Müşteri kimlik ve profil verileri |
| `accounts` | `BIGINT (IDENTITY)` | `account_number UNIQUE`, `CHECK (balance >= 0)`, `@Version` | Vadesiz/Vadeli banka hesapları ve bakiye |
| `transactions` | `UUID` | `idempotency_key UNIQUE`, `reference_number UNIQUE` | Finansal operasyon üst bilgisi ve Idempotency takibi |
| `ledger_entries` | `BIGINT (IDENTITY)` | `updatable = false`, `CHECK (amount > 0)` | Çift taraflı borç (`DEBIT`) ve alacak (`CREDIT`) defteri |

---

## 🛠️ Teknoloji Yığını

| Katman / Kütüphane | Versiyon / Teknoloji | Seçim Gerekçesi ve Rolü |
| :--- | :--- | :--- |
| **Dil** | Java 21 LTS | Pattern Matching, Records, Thread-safe concurrent pratikleri |
| **Framework** | Spring Boot 4.1.2 | Kurumsal servis altyapısı, Declarative Transaction Management |
| **Veri Katmanı** | Spring Data JPA / Hibernate | ORM yönetimi, Pessimistic & Optimistic Concurrency Controls |
| **Veritabanı** | PostgreSQL 16 | ACID standartları, Row-Level Locking (`SELECT FOR UPDATE`), Strict Constraints |
| **Konteynerizasyon** | Docker Compose | PostgreSQL veritabanını izole ve kalıcı bir ortamda ayağa kaldırma |
| **Birim Testleri** | JUnit 5 & Mockito | 7 Güvenlik kapısı ve edge-case senaryolarının izole doğrulaması |
| **Entegrasyon Testi** | Testcontainers | Gerçek PostgreSQL üzerinde yüksek eşzamanlılık (concurrency) testleri |
| **Veri Göçü (Migration)** | Flyway (hazırlık) | Veritabanı şema versiyonlama ve DDL script yönetimi |
| **Kod Üretimi** | Lombok | `@Getter`, `@Builder`, `@RequiredArgsConstructor` ile boilerplate minimizasyonu |

---

## 📁 Proje Dizin Yapısı

Proje, katmanlı ve temiz mimari (Clean & Layered Architecture) prensiplerine göre yapılandırılmıştır:

```text
core-banking-ledger/
├── .mvn/                                # Maven wrapper dosyaları
├── src/
│   ├── main/
│   │   ├── java/com/akt/banking_v1/
│   │   │   ├── BankingV1Application.java # Spring Boot giriş noktası
│   │   │   ├── common/
│   │   │   │   └── exception/           # Finansal domain istisna hiyerarşisi
│   │   │   │       ├── BankingException.java
│   │   │   │       ├── InsufficientBalanceException.java
│   │   │   │       ├── AccountNotFoundException.java
│   │   │   │       ├── AccountInactiveException.java
│   │   │   │       ├── CurrencyMismatchException.java
│   │   │   │       ├── SelfTransferException.java
│   │   │   │       └── CustomerAlreadyExistsException.java
│   │   │   ├── config/
│   │   │   │   └── DataInitializer.java # Sistem Müşterisi ve Kasa Hesabı başlangıç verisi
│   │   │   ├── dto/                     # Jakarta Validation korumalı Record DTO'lar
│   │   │   │   ├── TransferRequest.java / TransferResponse.java
│   │   │   │   ├── CreateAccountRequest.java / AccountResponse.java
│   │   │   │   ├── CreateCustomerRequest.java / CustomerResponse.java
│   │   │   │   └── DepositRequest.java
│   │   │   ├── enums/                   # Tip güvenli finansal durumlar
│   │   │   │   ├── Currency.java (TRY, USD, EUR)
│   │   │   │   ├── AccountStatus.java (ACTIVE, SUSPENDED, CLOSED)
│   │   │   │   ├── AccountType.java (CHECKING, SAVINGS)
│   │   │   │   ├── TransactionType.java (INTERNAL_TRANSFER, DEPOSIT, WITHDRAW)
│   │   │   │   ├── TransactionStatus.java (PENDING, COMPLETED, FAILED)
│   │   │   │   └── EntryType.java (DEBIT, CREDIT)
│   │   │   ├── mapper/                  # İzole ve test edilebilir DTO mappers
│   │   │   │   ├── TransferMapper.java
│   │   │   │   ├── AccountMapper.java
│   │   │   │   └── CustomerMapper.java
│   │   │   ├── model/                   # JPA Entity modelleri (Double-Entry domain)
│   │   │   │   ├── Customer.java
│   │   │   │   ├── Account.java
│   │   │   │   ├── Transaction.java
│   │   │   │   └── LedgerEntry.java
│   │   │   ├── repository/              # Spring Data JPA & Concurrency Lock Queries
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   ├── AccountRepository.java (findByAccountNumberWithLock)
│   │   │   │   ├── TransactionRepository.java (findByIdempotencyKey)
│   │   │   │   └── LedgerEntryRepository.java (findByAccountIdAndDateRange)
│   │   │   └── service/                 # Finansal iş mantığı motorları
│   │   │       ├── TransferService.java # Transfer, Deposit, Lock Ordering, Idempotency
│   │   │       ├── AccountService.java  # Hesap açılışı, 26 haneli TR IBAN üretimi
│   │   │       └── CustomerService.java # Müşteri onboarding ve tekillik kontrolleri
│   │   └── resources/
│   │       └── application.properties   # Veritabanı ve Hibernate ayarları
│   └── test/
│       └── java/com/akt/banking_v1/
│           ├── BankingV1ApplicationTests.java
│           ├── TestcontainersConfiguration.java
│           └── service/
│               └── TransferServiceTest.java # 8 Kapsamlı Mockito Birim Test Senaryosu
├── docker-compose.yml                   # PostgreSQL 16 servis tanımı
├── pom.xml                              # Maven bağımlılıkları ve konfigürasyonlar
├── core_banking_case_study_specification.md # Proje teknik şartnamesi
├── DATABASE_SCHEMA.md                   # Veritabanı ve DDL dokümantasyonu
├── progress.md                          # Proje iterasyon ve tamamlanma günlüğü
└── *.html                               # İnteraktif görsel mühendislik rehberleri
```

---

## ⚡ Temel Servisler ve İş Mantığı Akışı

### 1. `TransferService` (Transfer & Para Girişi Motoru)
* **`transfer(TransferRequest, idempotencyKey)`:**
  1. **Idempotency Denetimi:** `transactionRepository.findByIdempotencyKey(key)` ile sorgulanır. Kayıt varsa işlem tekrarlanmaz, önbellekteki işlem `TransferResponse` olarak dönülür.
  2. **Self-Transfer Koruması:** Gönderen ve alıcı hesap aynı olamaz.
  3. **Deadlock-Free Lock Ordering:** Hesap numaraları karşılaştırılarak (`compareTo`) küçük olan hesap önce, büyük olan sonra `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) kilidiyle okunur.
  4. **Durum & Döviz Kontrolleri:** Her iki hesabın `ACTIVE` olduğu ve aynı para birimine sahip olduğu doğrulanır.
  5. **Bakiye Kontrolü:** Gönderen hesap bakiyesinin işlem tutarını karşıladığı teftiş edilir.
  6. **Bakiye Güncellemesi:** Gönderen bakiyesinden düşülür, alıcı bakiyesine eklenir.
  7. **Çift Taraflı Defter Satırları:** 1 adet `Transaction` başlığı altında 1 adet `DEBIT` ve 1 adet `CREDIT` satırı atomik olarak kaydedilir.
  8. **Dekont No Üretimi:** `TXN-YYYYMMDD-UUID8` formatında tekil referans numarası atanır.
* **`deposit(DepositRequest, idempotencyKey)`:**
  * Dışarıdan hesaba nakit yatırılması durumunda paranın kaynağı olarak **`DataInitializer.SYSTEM_TRY_ACCOUNT` (Kasa Hesabı)** kullanılır. Kasa borçlandırılır (`DEBIT`), müşteri hesabı alacaklandırılır (`CREDIT`).
  * Dekont referans numarası `DEP-YYYYMMDD-UUID8` formatında üretilir.

### 2. `AccountService` (Hesap Açılışı ve IBAN Motoru)
* **Otomatik IBAN Üretimi:** `ThreadLocalRandom` ile Türkiye bankacılık standartlarına uygun `TR` önekli 24 haneli rakam bloğu (`TR` + 24 hane = 26 karakter) üretilir.
* **Collision Guard:** Üretilen IBAN'ın benzersiz olduğu `existsByAccountNumber` ile doğrulanır.
* **Varsayılan Güvenlik:** Yeni açılan hesaplar daima `ACTIVE` statüde ve `0.0000` bakiye ile başlatılır.

### 3. `CustomerService` (Müşteri Onboarding)
* TCKN/Vergi No ve e-posta adreslerinin tekilliği uygulama katmanında savunmacı kontrollerle (`existsByIdentityNumber`, `existsByEmail`) teftiş edilir.

---

## 🧪 Test Stratejisi ve Doğrulama Disiplini

Sistem kalitesi ve finansal doğruluğu, **Mockito Birim Testleri** ve **Testcontainers** altyapısıyla garanti altına alınmıştır.

### `TransferServiceTest` Kapsamı (8/8 Başarılı)

```
[INFO] Running com.akt.banking_v1.service.TransferServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.253 s
[INFO] BUILD SUCCESS
```

| # | Test Metodu | Test Edilen Senaryo | Doğrulama & Teftiş Mekanizması |
| :---: | :--- | :--- | :--- |
| **1** | `transfer_whenValidRequest_shouldCompleteSuccessfully` | **Happy Path:** Standart iki hesap arası başarılı transfer | Doğal kilit sırası, kuruş hassasiyetli bakiye değişimi, `save` ve `toResponse` teftişi |
| **2** | `transfer_whenIdempotencyKeyIsPresent_shouldReturnExistingTransfer` | **Idempotency Kalkanı:** Mükerrer istek yakalama | Sıfır DB kilitleme (`never().findByAccountNumberWithLock`), anında eski yanıt dönüşü |
| **3** | `transfer_whenSenderAndReceiverSame_shouldThrowSelfTransferException` | **Self-Transfer Koruması:** Kendi kendine transfer engeli | Bellek içi hızlı fail, sıfır DB kilit ve sıfır transaction kaydı |
| **4** | `transfer_whenInsufficientBalance_shouldThrowInsufficientBalanceException` | **Yetersiz Bakiye Kontrolü:** Kilit sonrası bakiye koruması | `never().save(Transaction)` ile veri tabanına yarım kayıt yazılmadığının kanıtlanması |
| **5** | `transfer_whenAccountNotFound_shouldThrowAccountNotFoundException` | **Hesap Bulunamadı:** Erken çıkış (Fail-Fast) | `UnnecessaryStubbingException` uyumlu temiz mock yönetimi |
| **6** | `transfer_whenAccountIsInactive_shouldThrowAccountInactiveException` | **İnaktif Hesap Güvenliği:** Askıya alınmış (`SUSPENDED`) veya kapalı hesap | Transferin bloke edilmesi ve istisna fırlatılması |
| **7** | `transfer_whenCurrencyMismatch_shouldThrowCurrencyMismatchException` | **Döviz Uyuşmazlığı:** TRY ile USD/EUR arası transfer engeli | Farklı para birimlerinin kilit sonrası reddedilmesi |
| **8** | `deposit_whenValidRequest_shouldDepositSuccessfully` | **Kasa Para Girişi (Deposit):** Sistem kasasından alacaklandırma | Kasa hesabına `DEBIT`, müşteriye `CREDIT`, `DEP-` referans numarası teftişi |

---

## 📚 İnteraktif Mühendislik Rehberleri

Repozituvarda, projenin tasarım ve mimari kararlarını detaylandıran interaktif HTML görsel dokümanları bulunmaktadır:

* 📄 **[`transfer_service_architecture_and_workflow_guide.html`](./transfer_service_architecture_and_workflow_guide.html):** 5 Güvenlik Kapısı, Deadlock-Free Lock Ordering, Kütüphaneci & Snapshot Dirty Checking analojisi.
* 📄 **[`optimistic_vs_pessimistic_locking_guide.html`](./optimistic_vs_pessimistic_locking_guide.html):** Concurrency çatışma stratejileri, `@Version` mekanizması ve row-level `FOR UPDATE` karşılaştırması.
* 📄 **[`repository_and_locking_guide.html`](./repository_and_locking_guide.html):** Spring Data JPA kilit anotasyonları, pessimistic write query'leri ve transactional sınırları.
* 📄 **[`jpa_equals_and_hashcode_guide.html`](./jpa_equals_and_hashcode_guide.html):** Hibernate Proxy, Set güvenliği ve Business Key (`accountNumber`) tabanlı kimlik sözleşmesi.
* 📄 **[`unit_testing_and_mockito_mastery_guide.html`](./unit_testing_and_mockito_mastery_guide.html):** Mockito birim test felsefesi, `verify` kontrolleri ve savunmacı test mimarisi.

---

## 🚀 Kurulum ve Çalıştırma Kılavuzu

### Ön Gereksinimler
* **Java:** JDK 21 LTS veya üzeri
* **Maven:** 3.9+ (veya proje kök dizinindeki `./mvnw`)
* **Docker & Docker Compose:** PostgreSQL veritabanı servisi için

### 1. Repoyu Klonlayın
```bash
git clone https://github.com/AKeremT/core-banking-ledger.git
cd core-banking-ledger
```

### 2. Veritabanını Docker ile Ayağa Kaldırın
```bash
docker compose up -d
```
> PostgreSQL servisi `localhost:5432` üzerinde, `core_banking_ledger` veritabanı adı, `akt` kullanıcı adı ve `akt` parolasıyla hazır hale gelecektir.

### 3. Testleri Koşturun
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### 4. Uygulamayı Başlatın
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
> Uygulama ilk kez ayağa kalktığında `DataInitializer` devreye girerek **Sistem Müşterisi** ve 1 Milyar TL bakiyeli **Sistem Kasa Hesabını** (`TR000000000000000000000001`) otomatik olarak oluşturacaktır.

---

## 🗺️ Yol Haritası ve Gelecek Adımlar

- [x] Case study teknik şartnamesi ve domain analizi (`Double-Entry Bookkeeping`).
- [x] PostgreSQL 16 Docker Compose ortamının kurulması.
- [x] JPA Entity'leri, enum'lar, `@Version` ve `@CheckConstraint` kuralları.
- [x] Concurrency odaklı Repository katmanı (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
- [x] Jakarta Bean Validation destekli Record DTO'lar ve istisna hiyerarşisi.
- [x] Transfer motoru (`TransferService.transfer` ve `TransferService.deposit`).
- [x] Otomatik TR IBAN motoru ile Müşteri ve Hesap servisleri (`AccountService`, `CustomerService`).
- [x] Mockito ile 8 senaryolu kapsamlı birim test zırhı (`TransferServiceTest`).
- [ ] **REST Controller & Global Exception Handling:** HTTP katmanının (`@RestController`, `@ControllerAdvice`) tamamlanması.
- [ ] **Testcontainers Concurrency Stres Testi:** 100 eşzamanlı thread ile gerçek PostgreSQL üzerinde Race Condition ve Deadlock testi.
- [ ] **Finansal Ekstre ve Analiz Motoru:** Java Stream API (`groupingBy`, `reduce`) ile hesap hareketlerinin raporlanması.
- [ ] **Flyway Database Migration:** Şemanın otomatik versiyonlanması.

---

## 📝 Commit ve Geliştirme Standartları

Bu proje, [`developer_study_workflow_dsa_blueprint.md`](../developer_study_workflow_dsa_blueprint.md) yönergesinde belirtilen **Conventional Commits** standardına sıkı sıkıya bağlı kalınarak geliştirilmektedir:

* `feat(transfer): implement deadlock-free lock ordering for transfer pipeline`
* `test(transfer): add comprehensive mockito unit tests for transfer and deposit`
* `docs(readme): add extensive documentation with architecture diagrams and workflows`
* `chore(db): configure postgresql 16 docker compose and datasource`

---

## 👤 Geliştirici

* **Geliştirici:** Kerem ([@AKeremT](https://github.com/AKeremT))
* **Çalışma Modeli:** AI-Assisted Socratic Mentorship & Dual-Track Engineering Discipline
