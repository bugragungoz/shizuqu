# Telefonda test listesi (Galaxy A35, One UI 9, Android 17)

Sırayla gidin; her adımda "Beklenen" olmuyorsa durun, aşağıdaki
"Sorun olursa" kısmındaki bilgileri toplayın. Hiçbir adım telefona kalıcı bir
şey yapmaz; tümü [SAFETY.md](SAFETY.md) içinde listelenen ayarlarla sınırlı.

## 0. Hazırlık

| Adım | Beklenen |
|---|---|
| Orijinal Shizuku yüklüyse kaldırın | Kaldırılır (izinleri de silinir, normal) |
| Shizuqu APK'sını kurun | "Shizuqu" adıyla, terminal ikonlu olarak kurulur |
| Ayarlar > Güvenlik ve gizlilik > Otomatik Engelleyici açıksa kapatın | Kapalı |

## 1. Görünüm (servis kapalıyken de denenebilir)

| Adım | Beklenen |
|---|---|
| Uygulamayı açın | Sıcak siyah zemin, yeşil vurgu, ince çerçeveli kartlar (koyu tema açıksa) |
| Ayarlar > Görünüm > Renk paleti: sırayla Bugra, Material You, Çivit, Okyanus... | Her seçimde ekran yeniden çizilir, renkler değişir, seçili olanın üzerinde tik olur; geri dönünce ana ekran da yeni renkte |
| Sistem karanlık modunu açıp kapatın | Açık/koyu hâl doğru; Ayarlar > "Siyah gece teması" açıkken zemin tam siyah |
| Ana sayfa > ⋮ > Hakkında | Terminal ikonu, sürüm `13.7.0.r…`, "GitHub" ve "Shizuku" bağlantıları |

## 2. Kablosuz hata ayıklama ile başlatma (asıl test)

| Adım | Beklenen |
|---|---|
| Wi-Fi'ye bağlanın; Geliştirici seçenekleri > Kablosuz hata ayıklama > Aç > "İzin ver" | Açık |
| Uygulamada "Kablosuz hata ayıklama ile başlat" kartı > **Eşleştir**, bildirime 6 haneli kodu yazın | "Eşleştirme başarılı" bildirimi |
| **Başlat** | Siyah kayıt ekranı akar, sonunda "Service started" ve pencere 3 sn sonra kapanır |
| Ana ekran | "Shizuqu çalışıyor", altında "Sürüm 13.7, adb" |

## 3. Android 17 düzeltmeleri

| Adım | Beklenen |
|---|---|
| Shizuku kullanan bir uygulamayı açın (ör. App Ops, Hail, Canta, InstallWithOptions) ve izin isteyin | Shizuqu izin penceresi çıkar ("**Uygulama** uygulamasına ... izin verilsin mi?") |
| İzin verin, uygulamanın bir işlemini deneyin | Çalışır (Shizuku'nun Android 17'de bozulan asıl kısmı) |
| Shizuqu ana ekranı > "Uygulama yönetimi" kartı | Sayı 0 değil; karta dokununca liste **boş değil**, izin verdiğiniz uygulama paket adıyla görünür (Android 17'deki ana hata buydu) |
| O uygulama açıkken Shizuqu menüsünden "Shizuqu'yu durdur", sonra tekrar Başlat | Uygulama yeniden kapatılıp açılmadan bağlanır |
| Güvenli Klasör içinde Shizuku kullanan bir uygulama varsa onu deneyin | Bağlantı alır ve listede görünür |

## 4. Hızlı Ayarlar kutucuğu ve açılışta başlatma

| Adım | Beklenen |
|---|---|
| Bildirim panelini açın > kalem (düzenle) > "Shizuqu" kutucuğunu ekleyin | Kutucuk eklenir; servis çalışıyorsa "Çalışıyor" |
| Menüden "Shizuqu'yu durdur", sonra kutucuğa dokunun | "Başlatılıyor…", kısa süre sonra "Çalışıyor" (Wi-Fi açık olmalı) |
| Kilit ekranında kutucuğa dokunun | Önce kilidi açmanızı ister |
| (İsteğe bağlı) Ayarlar > Sistem açılışında başlat > aç, telefonu yeniden başlatın | Wi-Fi bağlanınca birkaç saniye "Shizuqu başlatılıyor" bildirimi, sonra servis çalışır |

## 5. Geri alma (test bitince)

* Menü > "Shizuqu'yu durdur", ya da telefonu yeniden başlatın: servis tamamen kapanır.
* İsterseniz Geliştirici seçenekleri > Kablosuz hata ayıklamayı kapatın.
* Uygulamayı kaldırmak her şeyi geri alır.

## Sorun olursa

1. Başlatma sırasında olduysa: kayıt ekranında sağ üstteki **Kaydı kopyala**
   (uygulama, Android ve cihaz sürümünü de ekler).
2. Hangi adımda, ne beklediniz, ne oldu; ekran görüntüsü.
3. Bilgisayar varsa: `adb logcat -d | grep -iE "shizuku|AdbMdns|BootStart"`.

Bunları GitHub'da "Bug report" şablonuyla açın ya da bana gönderin.
