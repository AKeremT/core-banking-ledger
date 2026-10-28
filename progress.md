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
    - İnteraktif görsel teknik dokümantasyonlar hazırlandı (`jpa_equals_and_hashcode_guide.html`, `repository_and_locking_guide.html`, `optimistic_vs_pessimistic_locking_guide.html`).
- [x] **Repository Katmanı ve Concurrency Sorguları (Öğrenci Tarafından Tamamlandı):**
  - `CustomerRepository`: Kimlik no (TCKN/VKN) ve e-posta tekillik sorguları.
  - `AccountRepository`: Concurrency ve Deadlock-free Lock Ordering için `@Lock(LockModeType.PESSIMISTIC_WRITE)` ile `findByIdWithLock` ve `findByAccountNumberWithLock`.
  - `TransactionRepository`: Idempotency denetimleri için `findByIdempotencyKey` ve `existsByIdempotencyKey`.
  - `LedgerEntryRepository`: Hesap hareketleri (`findByAccountIdOrderByCreatedAtDesc`), tarih aralıklı ekstre sorguları (`findByAccountIdAndDateRange`) ve işlem bazlı çift kayıt filtreleme (`findByTransactionId`).
- [x] **Transfer DTO'ları ve Özel İstisna (Exception) Hiyerarşisi (Öğrenci Tarafından Tamamlandı):**
  - Domain istisna hiyerarşisi (`BankingException`, `InsufficientBalanceException`, `AccountNotFoundException`, `AccountInactiveException`, `CurrencyMismatchException`, `SelfTransferException`).
  - Jakarta Bean Validation zırhlı `TransferRequest` ve `TransferResponse` record DTO'ları.
  - Sektör standardı `referenceNumber` takibi (`Transaction` entity, DTO ve repository metodları).
- [x] **Transfer Motoru ve Çift Taraflı Muhasebe Servisi (Öğrenci Tarafından Tamamlandı):**
  - `TransferMapper`: `@Component` olarak Stream API ile `DEBIT` ve `CREDIT` satırlarını filtreleyen izole mapper bileşeni.
  - `TransferService`:
    - Mükerrer isteklere karşı **Idempotency** koruması (`findByIdempotencyKey`).
    - Karşılıklı kilitlenmeyi önleyen **Deadlock-Free Lock Ordering** (Doğal alfabetik kilit sırası).
    - Bakiye, para birimi ve hesap statü kontrolleri.
    - Atomik **Double-Entry Bookkeeping** ($\sum DEBIT = \sum CREDIT$) ve `referenceNumber` üretimi.

---

## 🎯 Sıradaki Adımlar

- [ ] Hesap açılışı ve bakiye yönetimi için `AccountService` katmanının geliştirilmesi.
- [ ] Ekstre analizi için Java Stream API servis metotlarının geliştirilmesi.
- [ ] Concurrency, Deadlock ve Double-Entry testlerinin (Testcontainers ile) yazılması.
