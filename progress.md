# Proje İlerleme Günlüğü (Progress Log)

**Proje:** `core-banking-ledger` (banking-v1)  
**Odak:** Double-Entry Bookkeeping, Race Condition & Locking, Idempotency, Clean Architecture  
**Başlangıç Tarihi:** 26 Ekim 2026  

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
  - Java 21 & Spring Boot
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
    - Karşılıklı kilitlenmeyi önleyen **Deadlock-Free Lock Ordering** (Doğal alfabetik kilit sırası).
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
- [x] **Birim (Unit) Testleri (Mockito - Başlandı):**
  - `TransferServiceTest`: 
    - Happy Path: Başarılı transfer, sıralı kilitleme, kuruş hassasiyetli bakiye güncellemeleri ve `verify` kontrolleri.
    - Idempotency Kalkanı: Mükerrer isteklerde erken dönüş ve `never()` ile sıfır veritabanı kilit/kayıt teftişi.

---

## 🎯 Sıradaki Adımlar (Devam Edenler)

- [ ] **Birim (Unit) Testleri (Mockito):**
  - `TransferServiceTest`: Self-Transfer, Yetersiz Bakiye, Aktif Olmayan Hesap, Para Birimi Uyuşmazlığı, Hesap Bulunamadı testleri.
  - `AccountServiceTest`: Müşteri bulunamadı kontrolü, IBAN üretim formatı testi.
  - `CustomerServiceTest`: Tekil TCKN/Email doğrulama testleri.
- [ ] **Entegrasyon & Concurrency Testleri (Testcontainers):**
  - Gerçek PostgreSQL konteyner üzerinde 100 eşzamanlı transfer ile Race Condition ve Deadlock testleri.
- [ ] **Ekstre & Finansal Analiz (Stream API):**
  - `LedgerEntry` hareketlerinin Java Stream API (`groupingBy`, `filter`, `reduce`) ile analiz edilmesi.
