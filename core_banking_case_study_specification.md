# Case Study: Yüksek Bütünlüklü Çekirdek Bankacılık Defteri (Core Banking Ledger)

## 1. Projenin Amacı ve Problem Tanımı
Geleneksel yazılımlarda bir kullanıcının bakiyesi `accounts.balance = accounts.balance - 100` şeklinde güncellenir. Ancak bankacılıkta bu yaklaşım kabul edilemez; çünkü:
1. **İzlenemezlik (Audit Failure):** Paranın nereden nereye gittiği, hangi işlemle çıktığı matematiksel olarak ispatlanamaz.
2. **Yarış Durumu (Race Condition):** İki farklı ATM aynı anda aynı bakiyeyi okursa para iki kez harcanabilir (Double Spending).
3. **Ağ Güvensizliği (Network Flakiness):** İstemcinin isteği zaman aşımına uğrayıp tekrar gönderildiğinde hesap 2 kez borçlandırılabilir.

Bu projede amacımız; **çift taraflı muhasebe (Double-Entry Bookkeeping)** kurallarını işleten, **dağıtık ağ hatalarına karşı idempotent** çalışan ve **yüksek eşzamanlılıkta (concurrency) kilitlenme yaşamayan** kurumsal bir transfer motoru inşa etmektir.

---

## 2. Uçtan Uca İş Akışı (End-to-End Business Flow)

Sistemimiz 4 ana operasyonel akış üzerinden çalışacaktır:

### Akış 1: Müşteri ve Hesap Açılışı (Onboarding)
* Bir müşteri sisteme TCKN/Vergi No, İsim ve İletişim bilgileriyle kaydedilir.
* Müşteriye ait bir veya birden fazla hesap açılabilir. Her hesabın:
  * Benzersiz bir IBAN/Hesap numarası,
  * Bir para birimi (TRY, USD, EUR),
  * Bir başlangıç statüsü (örn. `ACTIVE`) bulunur.
* **Kural:** Bir müşteri aynı para biriminde birden fazla vadesiz hesap açabilir. Bakiye ilk açılışta `0.0000` olmalıdır.

---

### Akış 2: Çift Taraflı Muhasebe ile Para Girişi (Deposit)
* Dışarıdan hesaba nakit yatırıldığında sistem "büyüyle para yaratmaz".
* **Kural:** Bankacılıkta paranın kaynağı ve hedefi daima bellidir. Dışarıdan para girerken sistem bir **Kasa/Sistem Hesabı (System Transit Account)** ile müşterinin hesabı arasında işlem yapar.
* Sonuç: Müşterinin hesabına `CREDIT` (Alacak), Sistem hesabına `DEBIT` (Borç) yazılır. Toplam borç-alacak dengesi sıfırdır ($\sum DEBIT = \sum CREDIT$).

---

### Akış 3: İki Hesap Arası Transfer (Kritik Yol - Critical Path)
Transfer motorunun kalbidir. Bir istemci (örneğin mobil uygulama) transfer talebi attığında sistem sırasıyla şu adımları işletmelidir:

```
[İstemci İsteği]
       │
       ▼
1. Idempotency Kontrolü ──(Var ve Başarılı mı?)──► [Eski Cevabı Anında Dön]
       │ (Yoksa)
       ▼
2. Validasyonlar (Para Birimi Eşleşmesi, Hesap Statüleri, Tutar > 0)
       │
       ▼
3. Kilit Sıralaması (Lock Ordering) & Eşzamanlılık Kontrolü
       │ (Deadlock'ı önlemek için hesaplar ID sırasına göre kilitlenir)
       ▼
4. Bakiye Yeterlilik Kontrolü
       │
       ▼
5. Atomik Defter Kayıtları (1 Transaction, 2 LedgerEntry: 1 DEBIT, 1 CREDIT)
       │
       ▼
6. Bakiye Güncellemesi & Idempotency Kaydının Tamamlanması
```

#### Bu Akıştaki Mühendislik Kuralları:
* **Idempotency Güvencesi:** İstemci her istekte HTTP Header'ında bir `Idempotency-Key` (UUID) göndermek zorundadır. Aynı key ile 1 milisaniye sonra bile ikinci bir istek gelse, transfer **kesinlikle tekrar çalıştırılmamalı**, ilk işlemin cevabı önbellekten/veritabanından dönülmelidir.
* **Deadlock Önleme (Lock Ordering):** 
  * A kullanıcısı B'ye para atarken (A kilitlendi -> B'yi bekliyor), aynı anda B kullanıcısı A'ya para atarsa (B kilitlendi -> A'yı bekliyor) **Deadlock** oluşur ve sistem kilitlenir.
  * *Tasarım Kuralı:* Hangi hesabın önce kilitleneceğine gönderen/alan sırasına göre değil; hesapların veritabanındaki ID veya IBAN değerlerinin **doğal sıralamasına (Natural Order - örn: küçük ID önce kilitlenir)** göre karar verilmelidir!
* **Bütünlük (Zero Data Loss):** İşlem sırasında hata olursa (örneğin bakiye yetersizse veya sunucu çökerse) veritabanı `@Transactional` sınırları içinde tamamen geri alınmalı (rollback), yarım kalmış kayıt bırakılmamalıdır.

---

### Akış 4: Ekstre ve Finansal Analiz (Stream API Pratiği)
* Kullanıcı bir hesabının belirli bir tarih aralığındaki hareketlerini ister.
* Veritabanından ham hareketler çekildikten sonra **Service katmanında Java Stream API kullanılarak**:
  1. Toplam giren para (`CREDIT` toplamı) ve çıkan para (`DEBIT` toplamı) hesaplanmalıdır.
  2. İşlemler kategorilerine göre (Deposit, Withdrawal, Transfer) gruplanmalıdır (`Collectors.groupingBy`).
  3. Tarihe göre sıralı ve sayfalanmış (paginated) bir ekstre DTO'su üretilmelidir.

---

## 3. İhtiyaç Duyulan Modeller ve Varlıklar (Domain Specifications)

Projenizde oluşturmanız gereken temel yapılar:

1. **`Customer`:** Müşteri kimlik ve profil verileri.
2. **`Account`:** Para birimi, bakiye, statü ve **optimistic lock versiyonu** (`@Version`).
3. **`Transaction`:** Transferin ana üst bilgisi (UUID, açıklama, tip, statü, referans numarası).
4. **`LedgerEntry`:** Değişmez (Immutable) borç/alacak hareket satırları.
5. **`IdempotencyRecord`:** Gelen isteklerin key'lerini, istek özetini (hash) ve verilen HTTP cevabını saklayan mekanizma.

---

## 4. Kabul Kriterleri (Acceptance Criteria)

Bu case study'nin başarılı sayılması için:
* [ ] Veritabanında hiçbir zaman negatif bakiye kalmamalıdır (Veritabanı CHECK constraint'i ile korunmalı).
* [ ] Asla bir transferde tek taraflı kayıt oluşmamalıdır (DEBIT tutarı = CREDIT tutarı).
* [ ] 100 eşzamanlı thread aynı hesaptan para çekmeye çalıştığında bakiye kuruşu kuruşuna doğru kalmalıdır (Testcontainers concurrency testi ile kanıtlanacak).
* [ ] Bütün finansal hesaplamalarda `BigDecimal` kullanılmalı ve `RoundingMode.HALF_EVEN` (Banker's Rounding) gözetilmelidir.
* [ ] Kod katmanları (Controller, Service, Repository, DTO, Mapper, Exception Handling) sektör standartlarında temiz mimariye uygun olmalıdır.
