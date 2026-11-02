# Proje İlerleme Günlüğü (Progress Log)

**Proje:** `core-banking-ledger` (banking-v1)  
**Odak:** Double-Entry Bookkeeping, Race Condition & Locking, Idempotency, Clean Architecture  
**Başlangıç Tarihi:** 26 Ekim 2026  
**Son Güncelleme:** 2 Kasım 2026  

> [!IMPORTANT]
> **ÖĞRENME VE ÇALIŞMA METODOLOJİSİ (MENTORLUK KURALI - ASLA UNUTULMAYACAK):**
> 1. **Kod Yazarı:** Kodları **daima kullanıcı (öğrenci)** yazar.
> 2. **Mentor Rolü:** Yapay zeka öğretmendir; gereksinimleri, sektör standartlarını ve mantığı anlatır, yönergeleri verir ve kullanıcının yazmasını bekler. Doğrudan kod yazıp dosyaya basmaz!
> 3. **İnceleme & Geri Bildirim:** Kullanıcı kodlamayı bitirdiğinde mentor kodları inceler, eksik/hata analizi yapar, Big-O ve best-practice denetiminden geçirir.

---

## 📌 Şu Ana Kadar Tamamlananlar

- [x] **Çalışma Metodolojisi ve Felsefe:**
  - `developer_study_workflow_dsa_blueprint.md` kılavuzu incelendi; Sokratesçi mentorluk, Big-O denetimi ve Conventional Commits kuralları benimsendi.
  - Sektör standartlarında öğrenme yaklaşımı (LLD önce, evrimsel HLD) kararlaştırıldı.
- [x] **Teknoloji Yığını ve Bağımlılıkların Belirlenmesi:**
  - Java 21 LTS & Spring Boot 4.1.2.
  - Maven bağımlılıkları: Spring Data JPA, Web, Validation, PostgreSQL, Flyway, Lombok, Testcontainers.
  - Spring Security'nin çekirdek iş mantığı ve concurrency oturduktan sonra eklenmesi kararlaştırıldı.
- [x] **Proje Tanımı ve Sistem Tasarımı Şartnamesi:**
  - Case Study şartnamesi (`core_banking_case_study_specification.md`) oluşturuldu.
  - Double-Entry Bookkeeping mantığı ($\sum DEBIT = \sum CREDIT$), Deadlock önleyici sıralı kilit mekanizması (Lock Ordering), Idempotency mimarisi ve Stream API gereksinimleri netleştirildi.
- [x] **Proje İskeletinin Kurulması:**
  - `start.spring.io` üzerinden Maven projesi oluşturuldu.
  - Klasör adı ve artifact ID sektör standardına uygun olarak `core-banking-ledger` olarak güncellendi.
- [x] **Git Reposunun Başlatılması ve İlk Commit:**
  - `git init -b main` çalıştırıldı.
  - İlk commit atıldı: `chore: initialize core banking ledger spring boot project with flyway and testcontainers`.
- [x] **GitHub Remote Reposunun Bağlanması:**
  - `origin` (`https://github.com/AKeremT/core-banking-ledger.git`) eklendi ve `main` branch'i GitHub'a başarıyla push edildi.
- [x] **Veritabanı Altyapısı ve Konfigürasyon:**
  - `docker-compose.yml` ile PostgreSQL 16 servisi kuruldu (named volume ile kalıcılık sağlandı).
  - `application.properties` yapılandırıldı (PostgreSQL connection, hibernate ddl-auto, sql formatlama).
- [x] **Domain Modellemesi ve Entity Tasarımı (Sektör Standardı):**
  - Enum sınıfları oluşturuldu (`Currency`, `AccountType`, `AccountStatus`, `TransactionType`, `TransactionStatus`, `EntryType`).
  - Çift taraflı muhasebe (Double-Entry) entity'leri kodlandı (`Customer`, `Account`, `Transaction`, `LedgerEntry`).
  - **Uygulanan Best Practice'ler:**
    - Optimistic Locking için `@Version` eklendi.
    - Kimlik numaraları için `String` tipi ve `unique` constraint'ler tanımlandı.
    - `@CreationTimestamp` ve `@UpdateTimestamp` ile otomatik UTC zaman yönetimi (`Instant`) kuruldu.
    - `@Builder.Default` ile koleksiyon başlatmaları yapılarak `NullPointerException` riski sıfırlandı.
    - Finansal değişmezlik için `LedgerEntry` kolonlarına `updatable = false` kısıtları uygulandı.
    - Hibernate Proxy ve Set güvenliği için Business Key (`accountNumber`) tabanlı `equals()` ve `getClass().hashCode()` uygulandı.
    - JPA 3.2 standardı ile veritabanı seviyesinde `@CheckConstraint` kuralları tanımlandı (`balance >= 0`, `amount > 0`).
- [x] **Repository Katmanı ve Concurrency Sorguları (Öğrenci Tarafından Tamamlandı):**
  - `CustomerRepository`: Kimlik no (TCKN/VKN) ve e-posta tekillik sorguları.
  - `AccountRepository`: Concurrency ve Deadlock-free Lock Ordering için `@Lock(LockModeType.PESSIMISTIC_WRITE)` ile `findByIdWithLock` ve `findByAccountNumberWithLock`.
  - `TransactionRepository`: Idempotency denetimleri için `findByIdempotencyKey` ve `existsByIdempotencyKey`.
  - `LedgerEntryRepository`: Hesap hareketleri (`findByAccountIdOrderByCreatedAtDesc`), tarih aralıklı ekstre sorguları (`findByAccountIdAndDateRange`) ve işlem bazlı çift kayıt filtreleme (`findByTransactionId`).
- [x] **Transfer DTO'ları ve Özel İstisna (Exception) Hiyerarşisi (Öğrenci Tarafından Tamamlandı):**
  - Domain istisna hiyerarşisi (`BankingException`, `InsufficientBalanceException`, `AccountNotFoundException`, `AccountInactiveException`, `CurrencyMismatchException`, `SelfTransferException`, `CustomerNotFoundException`, `CustomerAlreadyExistsException`).
  - Jakarta Bean Validation zırhlı `TransferRequest`, `TransferResponse`, `CreateAccountRequest`, `AccountResponse`, `CreateCustomerRequest`, `CustomerResponse`, `DepositRequest` record DTO'ları.
  - Sektör standardı `referenceNumber` takibi (`Transaction` entity, DTO ve repository metodları).
- [x] **Transfer Motoru ve Çift Taraflı Muhasebe Servisi (Öğrenci Tarafından Tamamlandı):**
  - `TransferMapper`: `@Component` olarak Stream API ile `DEBIT` ve `CREDIT` satırlarını filtreleyen izole mapper bileşeni.
  - `TransferService.transfer`:
    - Mükerrer isteklere karşı **Idempotency** koruması (`findByIdempotencyKey`).
    - Karşılıklı kilitlenmeyi önleyen **Deadlock-Free Lock Ordering** (Doğal alfabetik kilit sırası: `compareTo`).
    - Bakiye, para birimi ve hesap statü kontrolleri.
    - Atomik **Double-Entry Bookkeeping** ($\sum DEBIT = \sum CREDIT$) ve `referenceNumber` üretimi.
- [x] **Hesap Açılışı ve Yönetim Servisi (Öğrenci Tarafından Tamamlandı):**
  - `AccountMapper`: DTO dönüşümünü izole eden bağımsız `@Component`.
  - `AccountService`:
    - Sınıf seviyesinde `@Transactional(readOnly = true)` ile Hibernate dirty-checking optimizasyonu ve `LazyInitializationException` koruması.
    - Metot seviyesinde `@Transactional(rollbackFor = Exception.class)` ile atomik kayıt.
    - `ThreadLocalRandom` ile Türkiye standardında 24 haneli rakam bloğu içeren IBAN motoru (`TR` + 24 hane = 26 karakter).
    - `existsByAccountNumber` ile döngüsel çakışma koruması (collision guard).
    - `status(AccountStatus.ACTIVE)` ve başlangıç bakiyesi `BigDecimal.ZERO` ile güvenli hesap oluşturma.
- [x] **Müşteri Kayıt ve Onboarding Servisi (Öğrenci Tarafından Tamamlandı):**
  - `CustomerMapper`: Bağımsız `@Component` DTO dönüştürücü.
  - `CustomerService`:
    - `@Transactional(readOnly = true)` ve `@Transactional(rollbackFor = Exception.class)` mimarisi.
    - Savunmacı tekillik kontrolleri (`existsByIdentityNumber`, `existsByEmail`) ile veritabanı 500 hatalarını önleme.
    - Kimlik no ve ID bazlı müşteri sorgulama metotları.
- [x] **Para Girişi (Deposit) ve Sistem Kasa Mimarisi (Öğrenci Tarafından Tamamlandı):**
  - Altyapı: `DataInitializer` (`CommandLineRunner`) ile idempotent Sistem Müşterisi ("SYSTEM", "BANK_VAULT") ve 1 Milyar TL sermayeli Kasa Hesabı (`SYSTEM_TRY_ACCOUNT`).
  - `TransferService.deposit`:
    - Idempotency denetimi.
    - Kasa ve Müşteri hesapları arasında Deadlock-Free Lock Ordering (alfabetik sıralı `PESSIMISTIC_WRITE`).
    - Statü, para birimi ve bakiye doğrulamaları.
    - Çift taraflı muhasebe: Kasa hesabı borçlandırılır (`DEBIT`), müşteri hesabı alacaklandırılır (`CREDIT`). Toplam borç-alacak dengesi ($\sum DEBIT = \sum CREDIT$) korunur.
    - `DEP-` önekli dekont / referans numarası üretimi.
- [x] **Teknik Görsel Rehberler ve Eğitim Dokümantasyonu:**
  - `jpa_equals_and_hashcode_guide.html`: JPA entity'lerinde equals/hashCode sözleşmesi, Hibernate proxy ve Set güvenliği.
  - `repository_and_locking_guide.html`: Pessimistic vs Optimistic locking, SQL FOR UPDATE analizi.
  - `optimistic_vs_pessimistic_locking_guide.html`: Çakışma stratejileri ve @Version mekaniği.
  - `transfer_service_architecture_and_workflow_guide.html`: 5 Güvenlik Kapısı, Deadlock-Free Lock Ordering, Kütüphaneci & Snapshot Dirty Checking analojisi.
  - `unit_testing_and_mockito_mastery_guide.html`: Mockito birim test mantığı, 7 güvenlik kapısı senaryosu, UnnecessaryStubbing dersi ve finansal test püf noktaları.
- [x] **Birim (Unit) Testleri (Mockito - Transfer & Deposit Modülü Tamamlandı):**
  - `TransferServiceTest` (8 Senaryo - 8/8 Başarılı): 
    - **Transfer Metodu (7/7 Başarılı):**
      - Happy Path: Başarılı transfer, alfabetik sıralı kilit, kuruş hassasiyetli bakiye güncellemeleri ve `verify` kontrolleri.
      - Idempotency Kalkanı: Mükerrer isteklerde erken dönüş ve `never()` ile sıfır veritabanı kilit/kayıt teftişi.
      - Self-Transfer Koruması: Lambda tabanlı `assertThrows(SelfTransferException.class)` ile bellek içi doğrulama.
      - Yetersiz Bakiye: Kilit mekanizması sonrası bakiye kontrolü ve `never().save()` teftişi.
      - Hesap Bulunamadı: Fail-Fast (erken çıkış) mimarisi ve `UnnecessaryStubbingException` uyumlu temiz test.
      - İnaktif Hesap: `SUSPENDED` ve `CLOSED` hesap statülerinin engellenmesi.
      - Döviz Uyuşmazlığı: Farklı para birimlerine sahip hesaplar arası transfer blokajı.
    - **Deposit Metodu (1/1 Başarılı):**
      - Happy Path: Sistem Kasa Hesabı (`SYSTEM_TRY_ACCOUNT`) borçlandırma (`DEBIT`), müşteri hesabı alacaklandırma (`CREDIT`), çift taraflı bakiye teftişi ve `DEP-` dekont doğrulaması.
- [x] **Çift Dilli Kapsamlı Dokümantasyon & GitHub Senkronizasyonu (1 Kasım 2026):**
  - Finans ve fintech mühendisliği standartlarında uluslararası İngilizce `README.md` hazırlandı (Problem tanımı, 5 Güvenlik Kapısı, Double-entry dengesi, Lock Ordering, Mermaid ERD & akış şemaları, Mockito test tablosu, Docker çalıştırma talimatları).
  - Hazırlanan detaylı Türkçe dokümantasyon `README-tr.md` olarak korundu ve iki dosya arasına çift yönlü dil geçiş bağlantıları yerleştirildi.
  - Değişiklikler Conventional Commits kurallarına uygun olarak GitHub'a commit ve push edildi (`origin/main`).
- [x] **REST Controller ve Sunum (Presentation) Katmanı (2 Kasım 2026):**
  - `CustomerController`: Müşteri kayıt (`POST /api/v1/customers` -> 201 Created) ve TCKN/ID bazlı sorgulama endpoint'leri (`GET /api/v1/customers/{id}`).
  - `AccountController`: Hesap açılışı (`POST /api/v1/accounts` -> 201 Created), hesap no ve müşteri ID ile listeleme endpoint'leri.
  - `TransferController`: 
    - Para transferi (`POST /api/v1/transfers`) -> `@RequestHeader("Idempotency-Key")` zorunlu kalkanı.
    - Para girişi / nakit yatırma (`POST /api/v1/transfers/deposit`) -> `@RequestHeader("Idempotency-Key")` koruması.
  - `GlobalExceptionHandler` (`@RestControllerAdvice`):
    - OWASP uyumlu merkezi hata yönetimi: 500 Internal Server Error'da istemciye jenerik sabit mesaj dönülürken `log.error` ile iç detayların kaydedilmesi.
    - Polimorfik hata yönetimi (`Exception ex`), `MissingRequestHeaderException` ve `DataIntegrityViolationException` için `ClassCastException` önlemleri.
    - Bean Validation (`MethodArgumentNotValidException`) alan bazlı Map dönüştürme, bozuk JSON (`HttpMessageNotReadableException`) ve hatalı parametre (`MethodArgumentTypeMismatchException`) koruması.
    - Sektör standardı `ErrorResponse` record DTO'su.
- [x] **Entegrasyon ve Concurrency Stres Testleri (Testcontainers - 2 Kasım 2026):**
  - `TransferConcurrencyIntegrationTest`: Docker üzerinde çalışan gerçek PostgreSQL 16 ile Testcontainers entegrasyonu (`@ServiceConnection`).
  - **Deadlock-Free Çapraz Transfer Testi (100 Thread):** 50 A->B ve 50 B->A eşzamanlı çapraz transfer; `compareTo` kilit sıralamasının (Lock Ordering) deadlock üretmediği ve bakiyelerin kuruşu kuruşuna korunduğu ispatlandı.
  - **Double-Spending ve Negatif Bakiye Koruması Testi (10 Thread):** 100 TL'lik hesaptan aynı anda 10x20 TL çekim talebinde tam 5 işlemin başarılı olduğu, 5 işlemin `InsufficientBalanceException` ile reddedildiği ve bakiyenin asla negatife düşmeyerek 0.00 TL'de kaldığı kanıtlandı.
  - `integration_testing_and_concurrency_guide.html`: Entegrasyon testlerinin anatomisi, Testcontainers mantığı ve concurrency analizi görselleştirildi.
  - `java_concurrency_mastery_roadmap.html`: Üst dizinde donanımdan Virtual Threads'e kadar uzanan kapsamlı Java Concurrency yol haritası dokümante edildi.
- [x] **Ekstre ve Finansal Analiz Motoru (Java Stream API - 2 Kasım 2026):**
  - `AccountStatementService`:
    - Tarih aralıklı (`findByAccountIdAndDateRange`) tek sorgulu optimize defter satırları çekimi.
    - Savunmacı tarih yönetimi: `startDate` ve `endDate` null ise otomatik son 30 gün varsayılanı (`ChronoUnit.DAYS`).
    - Java Stream API ile dönem içi toplam giren (`CREDIT`) ve çıkan (`DEBIT`) tutarların kuruş hassasiyetinde toplanması (`filter`, `reduce`).
    - İşlem türüne göre (`INTERNAL_TRANSFER`, `DEPOSIT`) harcama analitiği dökümü (`Collectors.groupingBy`, `Collectors.reducing`).
    - `StatementItemResponse` ve `AccountStatementResponse` record DTO'ları ile kronolojik ekstre üretimi.
    - Hareket olmayan dönemlerde hata fırlatmak yerine temiz boş ekstre dönülmesi (`items: []`, `totalCredit: 0.00`, `totalDebit: 0.00`).
  - `AccountController`: `GET /api/v1/accounts/{accountNumber}/statement` REST endpoint'inin açılması.

---

## 🎯 Sıradaki Adımlar (Sonraki Oturumda Devam Edilecekler)

Geliştirme yol haritamız doğrultusunda bir sonraki çalışma oturumunda ele alınacak maddeler:

### 1. Para Çekme (Withdrawal) Operasyonu
- [ ] `WithdrawRequest` record DTO'sunun oluşturulması (`@NotBlank sourceAccountNumber`, `@NotNull @DecimalMin("0.01") amount`).
- [ ] `TransferService.withdraw`:
  - Idempotency denetimi (`findByIdempotencyKey`).
  - Kasa ve Müşteri hesabı arasında Deadlock-Free Lock Ordering (alfabetik sıralı kilit).
  - Yetersiz bakiye (`InsufficientBalanceException`), inaktif hesap ve para birimi kontrolleri.
  - Çift taraflı muhasebe: Müşteri hesabı borçlandırılır (`DEBIT`), Sistem Kasa hesabı alacaklandırılır (`CREDIT`).
  - `WDR-` önekli dekont / referans numarası üretimi.
- [ ] `TransferController`: `POST /api/v1/transfers/withdraw` (`@RequestHeader("Idempotency-Key")`) endpoint'i.

### 2. Flyway Veritabanı Migrasyonu (Database Migration)
- [ ] Hibernate `ddl-auto=update` modundan Flyway versiyonlu migrasyona geçiş (`spring.flyway.enabled=true`, `ddl-auto=validate`).
- [ ] `src/main/resources/db/migration/V1__init_schema.sql`:
  - `customers`, `accounts`, `transactions`, `ledger_entries` fiziksel DDL script'i.
  - `CHECK (balance >= 0)` ve `CHECK (amount > 0)` kısıtları.
  - `idx_accounts_iban`, `idx_tx_idempotency_key`, `idx_ledger_created_at` B-Tree performans indeksleri.

### 3. API Dokümantasyonu (OpenAPI 3 / Swagger)
- [ ] `springdoc-openapi-starter-webmvc-ui` entegrasyonu.
- [ ] `http://localhost:8080/swagger-ui.html` üzerinden canlı interaktif API arayüzü.
- [ ] Endpoint'ler ve DTO'lar için OpenAPI açıklamaları.
