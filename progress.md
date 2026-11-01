# Proje İlerleme Günlüğü (Progress Log)

**Proje:** `core-banking-ledger` (banking-v1)  
**Odak:** Double-Entry Bookkeeping, Race Condition & Locking, Idempotency, Clean Architecture  
**Başlangıç Tarihi:** 26 Ekim 2026  
**Son Güncelleme:** 1 Kasım 2026  

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

---

## 🎯 Sıradaki Adımlar (Yarın Devam Edilecek Eksik Kısımlar)

README yol haritası ve sistem mimarisi doğrultusunda yarınki çalışma oturumunda ele alınacak geliştirme maddeleri:

### 1. REST Controller ve Presentation Katmanı
- [ ] **`CustomerController`:** Müşteri oluşturma (`POST /api/v1/customers`), ID ve TCKN ile sorgulama (`GET /api/v1/customers/{id}`).
- [ ] **`AccountController`:** Hesap açılışı (`POST /api/v1/accounts`), hesap no ve müşteri ID ile listeleme endpoint'leri.
- [ ] **`TransferController`:**
  - Para transferi (`POST /api/v1/transfers`) -> HTTP Header'dan zorunlu `@RequestHeader("Idempotency-Key") UUID idempotencyKey` alımı.
  - Para yatırma (`POST /api/v1/transfers/deposit`) -> HTTP Header'dan zorunlu `Idempotency-Key` kontrolü.
- [ ] **Global Exception Handling (`@RestControllerAdvice`):**
  - Domain istisnaları (`InsufficientBalanceException` -> 422 Unprocessable Entity, `AccountNotFoundException` -> 404, `AccountInactiveException` -> 409 Conflict, `SelfTransferException` -> 400 Bad Request, `CurrencyMismatchException` -> 400).
  - Jakarta Bean Validation hatalarının yakalanıp (`MethodArgumentNotValidException`) alan bazlı anlaşılır JSON formatına dönüştürülmesi.
  - Veritabanı tekillik ihlallerinin (`DataIntegrityViolationException`) 409 Conflict olarak sunulması.

### 2. Kalan Birim (Unit) Testlerinin Tamamlanması (Mockito)
- [ ] **`TransferServiceTest` (Kalan Deposit Senaryoları):** Idempotency kalkanı, inaktif hesap, para birimi uyuşmazlığı, hesap bulunamadı ve kasa yetersiz bakiye kontrolleri.
- [ ] **`AccountServiceTest`:** Müşteri bulunamadı durumu, 26 haneli TR IBAN regex ve format doğrulaması, IBAN çakışma (collision) simülasyonu.
- [ ] **`CustomerServiceTest`:** Mükerrer TCKN/Email ile kayıt denemesi (`CustomerAlreadyExistsException`), müşteri sorgulama başarı ve hata durumları.

### 3. Entegrasyon ve Concurrency Stres Testleri (Testcontainers)
- [ ] Testcontainers ile gerçek PostgreSQL 16 ayağa kaldırılarak entegrasyon testlerinin yazılması.
- [ ] `ExecutorService` ile 100 eşzamanlı thread koşturularak aynı hesaplar üzerinde çapraz transfer stres testi yapılması:
  - **Deadlock-Free Lock Ordering** mekanizmasının kilitlenme yaşamadığının ispatlanması.
  - **Double Spending** (çift harcama) açığının oluşmadığının ve bakiyenin kuruşu kuruşuna tutarlı kaldığının kanıtlanması.

### 4. Ekstre ve Finansal Analiz Motoru (Java Stream API)
- [ ] `AccountStatementService`: Belirli tarih aralığındaki `LedgerEntry` hareketlerinin çekilmesi.
- [ ] Java Stream API (`groupingBy`, `filter`, `reduce`) pratikleri:
  - Toplam giren (`CREDIT`) ve çıkan (`DEBIT`) tutarların hesaplanması.
  - İşlem türüne göre (Deposit, Withdrawal, Transfer) gruplama.
  - Sayfalanmış (paginated) ve tarih sıralı ekstre DTO'sunun üretilmesi.

### 5. Flyway Veritabanı Migrasyonu (Database Migration)
- [ ] Hibernate `ddl-auto=update` modundan Flyway versiyonlu migrasyona geçiş (`db/migration/V1__init_schema.sql`).
- [ ] `CHECK (balance >= 0)` ve `CHECK (amount > 0)` kısıtları ile performans indekslerinin fiziksel SQL script'inde tanımlanması.
