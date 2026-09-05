# 🎯 Macierz Eisenhowera

Aplikacja do zarządzania zadaniami według Macierzy Eisenhowera — podział zadań na cztery kwadranty według **ważności** i **pilności**:

| | 🔴 **Pilne** | 🔵 **Niepilne** |
|---|---|---|
| **❗ Ważne** | **Zrób teraz** — kryzysy, terminy, problemy | **Zaplanuj** — planowanie, rozwój, zdrowie |
| **· Nieważne** | **🟠 Oddeleguj** — przerwy, telefony, sprawy innych | **⚪ Eliminuj** — pożeracze czasu |

## ✨ Funkcje

- **Konta użytkowników** — rejestracja, logowanie, hasła haszowane (PBKDF2-SHA256), sesje JWT
- **Macierz 2×2** z czterema kwadrantami i osiami ważność/pilność
- **Drag & drop** — przeciąganie zadań między kwadrantami i zmiana kolejności
- **Terminy i powtarzanie** — codzienne/tygodniowe/miesięczne zadania (po ukończeniu automatycznie powstaje następna instancja)
- **AI-podpowiedź kwadrantu** — Gemini (klucz w `.dev.vars`) z automatycznym fallbackiem do lokalnego mechanizmu (analiza słów kluczowych PL/EN + terminów), gdy klucza brak lub sieć niedostępna
- **Pomodoro** — minutnik 25/5 z dźwiękiem, zapisem sesji i powiązaniem z zadaniem
- **Statystyki** — ukończone zadania w tygodniu, rozkład w kwadrantach, czas skupienia
- **Filtry i wyszukiwarka** — dziś / tydzień / po terminie / szukanie w tekście
- **Eksport / import JSON** — kopia zapasowa zadań
- **Dwujęzyczny interfejs** PL/EN z przełącznikiem 🇵🇱/🇬🇧
- **Ciemny i jasny motyw** 🌙/☀️, glassmorphism, animacje, pełna responsywność

## 🧰 Technologia

- **Frontend:** React 18 + TypeScript + Vite, dnd-kit, React Router
- **Backend:** Cloudflare Workers (Web Crypto API — bez zewnętrznych zależności)
- **Baza danych:** Cloudflare D1 (SQLite na brzegu sieci)
- **AI:** Google Gemini (`gemini-2.5-flash`) + lokalny mechanizm heurystyczny

Konto demo: **demo@matrix.app** / **demo1234**

## 🚀 Uruchomienie lokalnie

```bash
npm install
cp .dev.vars.example .dev.vars   # uzupełnij GEMINI_API_KEY (opcjonalnie)
npm run dev                      # http://localhost:5173
```

Darmowy klucz Gemini: https://aistudio.google.com/app/apikey — bez niego aplikacja w pełni działa, a podpowiedzi kwadrantu generuje mechanizm lokalny.

## ☁️ Wdrożenie na Cloudflare

```bash
# 1. Zaloguj się do Cloudflare
npx wrangler login

# 2. Utwórz bazę D1 i podaj jej ID w wrangler.jsonc (database_id)
npx wrangler d1 create eisenhower-db

# 3. Ustaw sekrety produkcyjne
npx wrangler secret put JWT_SECRET
npx wrangler secret put GEMINI_API_KEY

# 4. Uruchom migracje (produkcja) i wdróż
npx wrangler d1 execute eisenhower-db --remote --file=./migrations/0001_initial.sql
npm run deploy
```

Po migracji odpal dowolne żądanie API (np. rejestrację) — konto demo zostanie utworzone automatycznie.

## 📁 Struktura

```
worker/            # Cloudflare Worker (API + auth + D1 + AI)
  index.ts         # routing API REST
  auth.ts          # PBKDF2 + JWT (Web Crypto)
  db.ts            # schemat D1 + seeding konta demo
  ai.ts            # Gemini + mechanizm heurystyczny
src/
  pages/           # AuthPage, MatrixPage, StatsPage
  components/      # QuadrantColumn, TaskCard, TaskModal, PomodoroWidget, Navbar
  i18n.tsx         # słownik PL/EN
  auth.tsx         # kontekst sesji
  api.ts           # klient API
migrations/        # migracje D1
```
