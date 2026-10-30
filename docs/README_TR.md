# Shizuqu

Shizuku'nun Android 17 ile çalışan fork'u. Android 17'de orijinal Shizuku'da
izinli uygulamalar listesi boş görünüyor ve uygulamalar Shizuku'ya
bağlanamıyor; Shizuqu bu sorunları düzeltiyor. Shizuku kullanan uygulamalar
(aShell, SD Maid SE, Hail, Canta, ...) hiçbir değişiklik gerekmeden çalışır.

## Kurulum

1. Orijinal Shizuku'yu kaldırın. Paket adları aynı, imzaları farklı olduğu
   için ikisi birlikte kurulamaz.
2. [Shizuqu 13.7.0](../release/Shizuqu-v13.7.0.apk) APK'sını kurun.
3. Root yoksa:
   1. Geliştirici seçenekleri > **Kablosuz hata ayıklama**'yı açın ve bu
      Wi-Fi ağına izin verin.
   2. Uygulamada **Eşleştir**'e dokunun ve 6 haneli kodu bildirime yazın.
   3. **Başlat**'a dokunun.
4. Shizuku kullanan uygulamalara erişim iznini yeniden verin.

Samsung'da eşleştirme ve başlatma sırasında **Otomatik Engelleyici**
kapalı olmalı.

## Yeniden başlatmadan sonra

Root yoksa servis her yeniden başlatmada durur. Tekrar başlatmanın iki yolu
var:

* Ayarlar > **Hızlı Ayarlar kutucuğu ekle**. Bundan sonra bildirim
  panelinden tek dokunuşla başlatabilirsiniz (Android 13+).
* Ayarlar > **Sistem açılışında başlat**. İzin verilen Wi-Fi'ye
  bağlanınca kendiliğinden başlar.

## Güvenlik

Root yoksa servis `adb shell` yetkisiyle çalışır:

* Sistem bölümlerine, bootloader'a ve Knox'a dokunamaz.
* Yeniden başlatınca kapanır.
* İzin verdiğiniz uygulamalar aynı yetkiyi alır, bu yüzden yalnızca
  güvendiğiniz uygulamalara izin verin.
* Uygulamayı kaldırmak her şeyi geri alır.

## Sorun giderme

| Belirti | Çözüm |
|---|---|
| Eşleştirme servisi bulunamıyor | Wi-Fi'ye bağlanın, kablosuz hata ayıklamayı açıp bu ağa izin verin, Otomatik Engelleyici'yi kapatın |
| Başlat "bağlanılamıyor" diyor | Kablosuz hata ayıklamayı kapatıp açın, yeniden eşleştirin |
| Bir uygulama "Shizuku çalışmıyor" diyor | O uygulamayı kapatıp açın |

Başlatma başarısız olursa, başlatma ekranındaki **Kaydı kopyala** ile
kaydı alıp GitHub'da hata bildirimi açın.
