# Shizuqu

Shizuku'nun Android 17 ile çalışan fork'u. Android 17'de orijinal Shizuku'da
izinli uygulamalar listesi boş görünüyor ve uygulamalar Shizuku'ya
bağlanamıyor; Shizuqu bu sorunları düzeltiyor. Shizuku kullanan uygulamalar
(aShell, SD Maid SE, Hail, Canta, ...) hiçbir değişiklik gerekmeden çalışır.

> [!NOTE]
> Shizuqu resmi olmayan, geçici bir fork'tur. Shizuku'nun Android 17
> güncellemesi çıkmadan önce yapıldı. RikkaApps Android 17'yi destekleyen bir
> Shizuku sürümü yayınladığında resmi Shizuku'ya geri dönün. Shizuqu'nun
> RikkaApps veya Google ile bir bağlantısı yoktur.

## Kurulum

1. Orijinal Shizuku'yu kaldırın. Paket adları aynı, imzaları farklı olduğu
   için ikisi birlikte kurulamaz.
2. APK'yı [Releases](https://github.com/bugragungoz/shizuqu/releases/latest) sayfasından indirip kurun.
3. Root yoksa:
   1. Geliştirici seçenekleri > **Kablosuz hata ayıklama**'yı açın ve bu
      Wi-Fi ağına izin verin.
   2. Uygulamada **Eşleştirme**'ye dokunun ve 6 haneli kodu bildirime yazın.
   3. **Başlat**'a dokunun.
4. Shizuku kullanan uygulamalara erişim iznini yeniden verin.

Samsung'da eşleştirme ve başlatma sırasında **Otomatik Engelleyici**
kapalı olmalı.

Resmi Shizuku'ya dönmek için Shizuqu'yu kaldırın, Shizuku'yu kurun ve
uygulamalara izni yeniden verin.

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
