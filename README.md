# Fokus Budżet

Androidowa aplikacja do planowania i kontrolowania pieniędzy za pomocą macierzy Eisenhowera. Użytkownik dodaje wpływy, rozdziela je między cztery obszary i zapisuje wydatki. Każdy wydatek dostaje propozycję ćwiartki, ale ostateczna decyzja zawsze należy do użytkownika.

## Cztery obszary

- **Ważne i pilne** — rachunki, raty, obowiązki i sytuacje wymagające szybkiej reakcji.
- **Ważne i niepilne** — oszczędności, cele, inwestycje i edukacja.
- **Mniej ważne i pilne** — bieżące potrzeby i szybkie sprawy.
- **Mniej ważne i niepilne** — przyjemności oraz zachcianki.

## Zakres pierwszej wersji

- Kotlin + Jetpack Compose, Android 12+
- lokalna baza Room i tryb offline-first
- opcjonalne logowanie Google + e-mail dla synchronizacji bankowej
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

Aplikacja pozostaje offline-first: podstawowe dane budżetu są przechowywane lokalnie w Room. Opcjonalna synchronizacja bankowa korzysta z backendu Cloudflare Workers + D1 + R2 i dostawcy Salt Edge; Fokus nie przechowuje haseł bankowych i nie wykonuje przelewów ani płatności. Sugestie ćwiartki dla transakcji bankowej zawsze wymagają ręcznego zatwierdzenia.

Pierwszy etap backendu znajduje się w [`backend/`](backend/) i obejmuje adapter Salt Edge, migrację D1, ręczną/dzienną synchronizację oraz weryfikację podpisów callbacków. Połączenie backendu z ekranem Androida i przepływ logowania Google/e-mail wymagają jeszcze konfiguracji środowiska oraz sekretów poza repozytorium. Szczegóły decyzji dostawcy są w [`docs/open-banking-provider-comparison.md`](docs/open-banking-provider-comparison.md).

## Uruchomienie

Otwórz katalog w Android Studio Ladybug lub nowszym, pozwól Gradle pobrać zależności i uruchom konfigurację `app` na emulatorze lub urządzeniu z Androidem 12+.

Po skonfigurowaniu Gradle można uruchomić testy i build:

```bash
gradle :app:test
gradle :app:assembleDebug
```

W tym środowisku nie ma zainstalowanego JDK ani Android SDK, dlatego lokalny build zostanie wykonany po otwarciu projektu w Android Studio lub w CI.

## Automatyczne budowanie APK

Workflow `.github/workflows/android.yml` uruchamia się przy każdym pushu na `main` lub gałąź `arena/**`, przy Pull Requeście do `main` oraz ręcznie. Wykonuje testy, buduje debug APK i publikuje go jako artefakt GitHub Actions.

Po zakończeniu workflow wejdź w GitHubie w `Actions → Android APK → wybrane uruchomienie → Artifacts` i pobierz plik `fokus-debug-apk-*`.

## Backend synchronizacji bankowej

W katalogu `backend/`:

```bash
npm ci
npm run typecheck
npm test
npx wrangler d1 migrations apply fokus-bank-db --local
npm run dev
```

Prawdziwe wartości Salt Edge, klucz JWT oraz klucz publiczny callbacków należy ustawić jako sekrety Cloudflare lub lokalnie w `backend/.dev.vars` utworzonym na podstawie `backend/.dev.vars.example`. Nie wpisuj ich do Git. Workflow `.github/workflows/backend.yml` wykonuje typecheck i testy Workera.
