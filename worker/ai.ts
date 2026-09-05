import type { Env } from "./env";

export interface Suggestion {
  quadrant: "do" | "schedule" | "delegate" | "eliminate";
  confidence: number;
  reasons: string[];
  source: "gemini" | "heuristic";
}

const Q_LABEL: Record<string, { pl: string; en: string }> = {
  do: { pl: "Zrób teraz (ważne i pilne)", en: "Do now (urgent & important)" },
  schedule: { pl: "Zaplanuj (ważne, niepilne)", en: "Schedule (important, not urgent)" },
  delegate: { pl: "Oddeleguj (pilne, nieważne)", en: "Delegate (urgent, not important)" },
  eliminate: { pl: "Eliminuj (niepilne, nieważne)", en: "Eliminate (not urgent, not important)" },
};

// ---------- Lokalny mechanizm (fallback) ----------

const URGE_PL = ["pilne", "teraz", "natychmiast", "kryzys", "awaria", "termin", "deadline", "dziś", "dzisiaj", "jutro", "goni", "krytyczn", "błąd", "błędem", "płacić", "faktur", "egzamin", "oddanie", "ratować", "pożar", "wyciek", "skarga", "rezygnacja"];
const URGE_EN = ["urgent", "asap", "now", "immediately", "crisis", "deadline", "today", "tomorrow", "due", "overdue", "critical", "crash", "outage", "bug", "error", "pay", "invoice", "exam", "submit", "emergency", "fire", "complaint", "fix"];

const IMPORTANT_PL = ["rodzina", "zdrowie", "trening", "ćwicz", "firma", "biznes", "klient", "umowa", "rekrutac", "pensja", "budżet", "podatki", "strateg", "planow", "cele", "celów", "nauka", "kurs", "egzamin", "studia", "praca dyplomowa", "książk", "rozwój", "wizja", "dom", "mieszkan", "ślub", "rozliczen", "lekarz", "wizyta", "ubezpiecz", "oszczędnoś", "inwest"];
const IMPORTANT_EN = ["family", "health", "workout", "exercise", "business", "client", "contract", "hire", "salary", "budget", "tax", "strategy", "strategic", "plan", "goal", "learn", "course", "exam", "study", "thesis", "book", "growth", "vision", "house", "mortgage", "wedding", "doctor", "appointment", "insurance", "saving", "invest", "career"];

const DELEGATE_PL = ["prześlij dalej", "przekaż", "zadzwoń do mnie", "ktoś pyta", "prośba o", "może asystent", "zapytanie", "rezerwacja", "umówić wizytę", "ankieta"];
const DELEGATE_EN = ["forward", "call me", "someone asked", "can you", "please review my", "reschedule", "booking", "survey", "rsvp", "cc"];

const ELIMINATE_PL = ["scrollowanie", "media społecznościowe", "netflix", "serial", "granie", "gra komputerowa", "przeglądać", "bez celu", "odkurzyć", "starych plików", "pogaduszki", "plotki"];
const ELIMINATE_EN = ["scrolling", "social media", "netflix", "series", "gaming", "video game", "browsing", "mindless", "reorganize desk", "gossip", "cat videos"];

function containsAny(text: string, words: string[]): string | null {
  for (const w of words) if (text.includes(w)) return w;
  return null;
}

function parseDueDate(dueDate: string | null, t: "pl" | "en"): { days: number | null; urgent: boolean } {
  if (!dueDate) return { days: null, urgent: false };
  const due = new Date(dueDate + "T23:59:59");
  if (isNaN(due.getTime())) return { days: null, urgent: false };
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const target = new Date(due.getFullYear(), due.getMonth(), due.getDate());
  const days = Math.round((target.getTime() - today.getTime()) / 86_400_000);
  return { days, urgent: days <= 2 };
}

export function heuristicSuggest(title: string, notes: string, dueDate: string | null, lang: "pl" | "en"): Suggestion {
  const text = ` ${title} ${notes ?? ""} `.toLowerCase();
  const reasons: string[] = [];
  let urgent = 0;
  let important = 0;

  const uw = containsAny(text, lang === "pl" ? URGE_PL : URGE_EN);
  if (uw) {
    urgent += 2;
    reasons.push(lang === "pl" ? `Wykryto pilne słowo kluczowe: „${uw}”` : `Urgent keyword detected: “${uw}”`);
  }
  const iw = containsAny(text, lang === "pl" ? IMPORTANT_PL : IMPORTANT_EN);
  if (iw) {
    important += 2;
    reasons.push(lang === "pl" ? `Wykryto słowo związane z ważnym celem: „${iw}”` : `Important-goal keyword detected: “${iw}”`);
  }
  const dw = containsAny(text, lang === "pl" ? DELEGATE_PL : DELEGATE_EN);
  if (dw) {
    urgent += 1;
    important -= 1;
    reasons.push(lang === "pl" ? "Brzmi jak zadanie, które można przekazać komuś innemu" : "Sounds like something that can be delegated");
  }
  const ew = containsAny(text, lang === "pl" ? ELIMINATE_PL : ELIMINATE_EN);
  if (ew) {
    urgent -= 1;
    important -= 2;
    reasons.push(lang === "pl" ? "Wygląda na aktywność mało wartościową — kandydat do eliminacji" : "Looks like a low-value activity — elimination candidate");
  }

  const { days, urgent: dueUrgent } = parseDueDate(dueDate, lang);
  if (days !== null) {
    if (dueUrgent) {
      urgent += 2;
      reasons.push(lang === "pl" ? `Termin za ${days <= 0 ? "mniej niż dzień" : `${days} dni`} — pilne` : `Due in ${days <= 0 ? "under a day" : `${days} days`} — urgent`);
    } else {
      reasons.push(lang === "pl" ? `Termin za ${days} dni — jest czas na zaplanowanie` : `Due in ${days} days — can be scheduled`);
    }
  }

  let quadrant: Suggestion["quadrant"];
  if (urgent > 0 && important > 0) quadrant = "do";
  else if (urgent <= 0 && important > 0) quadrant = "schedule";
  else if (urgent > 0 && important <= 0) quadrant = "delegate";
  else quadrant = "eliminate";

  const signalCount = (uw ? 1 : 0) + (iw ? 1 : 0) + (dw ? 1 : 0) + (ew ? 1 : 0) + (days !== null ? 1 : 0);
  const confidence = Math.min(92, 45 + signalCount * 12 + Math.abs(urgent) * 4 + Math.abs(important) * 4);

  const label = Q_LABEL[quadrant][lang];
  reasons.push(lang === "pl" ? `Rekomendacja: ${label}` : `Recommendation: ${label}`);

  return { quadrant, confidence, reasons, source: "heuristic" };
}

// ---------- Gemini ----------

export async function geminiSuggest(
  title: string,
  notes: string,
  dueDate: string | null,
  lang: "pl" | "en",
  env: Env
): Promise<Suggestion | null> {
  if (!env.GEMINI_API_KEY) return null;
  const model = env.GEMINI_MODEL || "gemini-2.5-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${env.GEMINI_API_KEY}`;

  const sys = `You classify tasks into the Eisenhower Matrix. Quadrants:
- "do": urgent AND important (crises, deadlines today/tomorrow, critical problems)
- "schedule": important but NOT urgent (planning, learning, health, relationships, long-term goals)
- "delegate": urgent but NOT important (interruptions, some calls/messages, tasks others can do)
- "eliminate": NOT urgent and NOT important (time wasters, busywork, mindless scrolling)
Respond with JSON only: {"quadrant": one of do|schedule|delegate|eliminate, "confidence": number 1-99, "reasons": array of 2-3 short strings}.
All "reasons" text MUST be in ${lang === "pl" ? "Polish" : "English"}. Be decisive.`;

  const userMsg = `Task title: ${title}\nNotes: ${notes || "(none)"}\nDue date: ${dueDate || "(none)"}`;

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 9000);
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      signal: controller.signal,
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: sys }] },
        contents: [{ parts: [{ text: userMsg }] }],
        generationConfig: { responseMimeType: "application/json", temperature: 0.4, maxOutputTokens: 400 },
      }),
    });
    if (!res.ok) return null;
    const data = (await res.json()) as {
      candidates?: { content?: { parts?: { text?: string }[] } }[];
    };
    const text = data.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("") ?? "";
    const match = text.match(/\{[\s\S]*\}/);
    if (!match) return null;
    const parsed = JSON.parse(match[0]) as { quadrant?: string; confidence?: number; reasons?: string[] };
    if (!parsed.quadrant || !["do", "schedule", "delegate", "eliminate"].includes(parsed.quadrant)) return null;
    const label = Q_LABEL[parsed.quadrant][lang];
    const reasons = Array.isArray(parsed.reasons) ? parsed.reasons.slice(0, 3) : [];
    reasons.push(lang === "pl" ? `Rekomendacja AI: ${label}` : `AI recommendation: ${label}`);
    return {
      quadrant: parsed.quadrant as Suggestion["quadrant"],
      confidence: Math.max(50, Math.min(99, Math.round(parsed.confidence ?? 75))),
      reasons,
      source: "gemini",
    };
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

export async function suggestQuadrant(
  title: string,
  notes: string,
  dueDate: string | null,
  lang: "pl" | "en",
  env: Env
): Promise<Suggestion> {
  const gemini = await geminiSuggest(title, notes, dueDate, lang, env);
  if (gemini) return gemini;
  return heuristicSuggest(title, notes, dueDate, lang);
}
