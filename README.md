# Fokus

Androidowa aplikacja macierzy Eisenhowera — szybkie planowanie zadań według ważności i pilności.

## Założenia pierwszej wersji

- Kotlin + Jetpack Compose, Android 12+
- lokalna baza Room i działanie offline-first
- macierz 2×2 z automatyczną sugestią ćwiartki na podstawie terminu oraz ważności
- zadania z opisem, notatką, linkiem, projektem, tagami, terminem, cyklem i przypomnieniem
- lokalne przypomnienia Androida, archiwum wykonanych zadań i widget
- polski interfejs Material 3, jasny/ciemny motyw

Synchronizacja między urządzeniami, logowanie Google i pliki będą osobnym etapem. Planowana warstwa chmurowa może później korzystać z Cloudflare Workers, D1 i R2.

## Uruchomienie

Otwórz katalog w Android Studio Ladybug lub nowszym, pozwól Gradle pobrać zależności i uruchom konfigurację `app` na emulatorze/urządzeniu z Androidem 12+.

Po skonfigurowaniu Gradle można uruchomić testy i build:

```bash
gradle :app:test
gradle :app:assembleDebug
```

W tym środowisku nie ma zainstalowanego JDK ani Android SDK, dlatego lokalny build zostanie wykonany po otwarciu projektu w Android Studio lub w CI.
