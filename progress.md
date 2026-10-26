# Proje İlerleme Günlüğü (Progress Log)

**Proje:** `core-banking-ledger` (banking-v1)  
**Odak:** Double-Entry Bookkeeping, Race Condition & Locking, Idempotency, Clean Architecture  
**Başlangıç Tarihi:** 26 Ekim 2026  

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

---

## 🎯 Sıradaki Adımlar

- [ ] Repository katmanının oluşturulması (`CustomerRepository`, `AccountRepository`, `TransactionRepository`, `LedgerEntryRepository`).
- [ ] Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) sorgularının tasarlanması.
- [ ] Deadlock önleyici sıralı kilit (Lock Ordering) ve Idempotency kontrol iş mantığının Servis katmanında kurulması.
- [ ] Ekstre analizi için Java Stream API servis metotlarının geliştirilmesi.
- [ ] Concurrency ve Double-Entry testlerinin yazılması.
