# Fokus Budżet

Androidowa aplikacja do planowania i kontrolowania pieniędzy za pomocą macierzy Eisenhowera. Użytkownik dodaje wpływy, rozdziela je między cztery obszary i zapisuje wydatki. Każdy wydatek dostaje propozycję ćwiartki, ale ostateczna decyzja zawsze należy do użytkownika.

## Cztery obszary

- **Ważne i pilne** — rachunki, raty, obowiązki i sytuacje wymagające szybkiej reakcji.
- **Ważne i niepilne** — oszczędności, cele, inwestycje i edukacja.
- **Mniej ważne i pilne** — bieżące potrzeby i szybkie sprawy.
- **Mniej ważne i niepilne** — przyjemności oraz zachcianki.

## Zakres pierwszej wersji

- Kotlin + Jetpack Compose, Android 12+
- lokalna baza Room i brak logowania
- wpływy z wielu źródeł
- budżet z własnym dniem rozpoczęcia okresu
- wiele walut z wybraną walutą bazową
- odświeżanie kursu online i lokalny fallback na ostatni zapisany kurs
- proste wydatki: nazwa, kwota, waluta, data i ćwiartka
- transparentna propozycja ćwiartki dla nowego wydatku
- planowanie kwot dla czterech ćwiartek
- propozycja podziału na podstawie historii, zawsze wymagająca ręcznego zatwierdzenia
- ręczne przenoszenie środków między ćwiartkami z ostrzeżeniem
- widoki Budżet, Wydatki, Historia oraz widget Androida
- kreator pierwszego budżetu bez narzucania procentów
- jasny i ciemny motyw Material 3

Dane finansowe nie są wysyłane do konta ani synchronizowane. Internet jest używany wyłącznie opcjonalnie do odświeżania kursu walut. Synchronizacja między urządzeniami i eksport/backup mogą zostać dodane później.

## Uruchomienie

Otwórz katalog w Android Studio Ladybug lub nowszym, pozwól Gradle pobrać zależności i uruchom konfigurację `app` na emulatorze lub urządzeniu z Androidem 12+.

Po skonfigurowaniu Gradle można uruchomić testy i build:

```bash
gradle :app:test
gradle :app:assembleDebug
```

W tym środowisku nie ma zainstalowanego JDK ani Android SDK, dlatego lokalny build zostanie wykonany po otwarciu projektu w Android Studio lub w CI.
