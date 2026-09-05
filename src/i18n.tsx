import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

export type Lang = "pl" | "en";

const dict = {
  pl: {
    appName: "Macierz Eisenhowera",
    tagline: "Rób ważne rzeczy, zanim staną się pilne.",
    matrix: "Macierz",
    stats: "Statystyki",
    login: "Logowanie",
    register: "Rejestracja",
    logout: "Wyloguj się",
    email: "E-mail",
    password: "Hasło",
    name: "Imię (opcjonalnie)",
    signIn: "Zaloguj się",
    signUp: "Zarejestruj się",
    noAccount: "Nie masz konta?",
    haveAccount: "Masz już konto?",
    demoHint: "Konto demo: demo@matrix.app / demo1234",
    useDemo: "Wejście demo",
    loginFailed: "Logowanie nieudane — sprawdź dane.",
    registerFailed: "Rejestracja nie powiodła się.",
    emailTaken: "Ten e-mail jest już zajęty.",
    shortPassword: "Hasło musi mieć min. 6 znaków.",

    importantUrgent: "Zrób teraz",
    importantNotUrgent: "Zaplanuj",
    notImportantUrgent: "Oddeleguj",
    notImportantNotUrgent: "Eliminuj",
    doSub: "Ważne i pilne — kryzysy, terminy, problemy",
    scheduleSub: "Ważne, niepilne — planowanie, rozwój, zdrowie",
    delegateSub: "Pilne, nieważne — przerwy, telefony, sprawy innych",
    eliminateSub: "Nieważne, niepilne — pożeracze czasu",
    axisImportant: "Ważne →",
    axisUrgent: "Pilne →",

    addTask: "Dodaj zadanie",
    newTask: "Nowe zadanie",
    editTask: "Edytuj zadanie",
    title: "Tytuł zadania",
    notes: "Notatki",
    dueDate: "Termin",
    recurrence: "Powtarzanie",
    none: "Brak",
    daily: "Codziennie",
    weekly: "Co tydzień",
    monthly: "Co miesiąc",
    quadrant: "Kwadrant",
    save: "Zapisz",
    cancel: "Anuluj",
    delete: "Usuń",
    done: "Ukończone",
    completedTasks: "Ukończone zadania",
    showCompleted: "Pokaż ukończone",
    hideCompleted: "Ukryj ukończone",
    noTasks: "Brak zadań — dodaj pierwsze!",
    dragHint: "Przeciągnij zadania między kwadrantami",
    search: "Szukaj zadania…",
    filterAll: "Wszystkie",
    filterToday: "Na dziś",
    filterWeek: "Ten tydzień",
    filterOverdue: "Po terminie",

    aiSuggest: "Zaproponuj kwadrant (AI)",
    aiThinking: "Analizuję zadanie…",
    aiSource: "Sugeruje AI (Gemini)",
    aiSourceLocal: "Sugeruje mechanizm lokalny",
    applySuggestion: "Zastosuj sugestię",

    pomodoro: "Pomodoro",
    start: "Start",
    pause: "Pauza",
    resume: "Wznów",
    reset: "Reset",
    work: "Praca",
    break: "Przerwa",
    pomoDone: "Sesja ukończona! Zrobiono {minutes} min.",
    pomoLogged: "Zapisano sesję ({minutes} min)",
    focusOn: "Praca nad: {task}",
    minutesShort: "min",

    statsTitle: "Twoje statystyki",
    tasksInQuadrants: "Otwarte zadania w kwadrantach",
    completedThisWeek: "Ukończone w tym tygodniu",
    pomodorosThisWeek: "Pomodoro w tym tygodniu",
    pomodorosTotal: "Łącznie minut skupienia",
    completedByQuadrant: "Ukończone wg kwadrantów",
    balanceHint: "Wskazówka: najwięcej czasu powinny zabierać zadania z kwadrantu „Zaplanuj” — to inwestycja w przyszłość.",

    export: "Eksport JSON",
    import: "Import JSON",
    imported: "Zaimportowano {n} zadań",
    importFailed: "Import nie powiódł się — sprawdź plik.",
    theme: "Motyw",
    light: "Jasny",
    dark: "Ciemny",
    lang: "Język",
    overdue: "Po terminie",
    today: "Dziś",
    tomorrow: "Jutro",
    inDays: "za {n} dni",
    repeatDaily: "codziennie",
    repeatWeekly: "co tydzień",
    repeatMonthly: "co miesiąc",
    confirmDelete: "Usunąć to zadanie?",
  },
  en: {
    appName: "Eisenhower Matrix",
    tagline: "Do the important things before they become urgent.",
    matrix: "Matrix",
    stats: "Stats",
    login: "Sign in",
    register: "Sign up",
    logout: "Log out",
    email: "Email",
    password: "Password",
    name: "Name (optional)",
    signIn: "Sign in",
    signUp: "Create account",
    noAccount: "No account yet?",
    haveAccount: "Already have an account?",
    demoHint: "Demo account: demo@matrix.app / demo1234",
    useDemo: "Use demo account",
    loginFailed: "Sign-in failed — check your credentials.",
    registerFailed: "Registration failed.",
    emailTaken: "This email is already taken.",
    shortPassword: "Password must be at least 6 characters.",

    importantUrgent: "Do now",
    importantNotUrgent: "Schedule",
    notImportantUrgent: "Delegate",
    notImportantNotUrgent: "Eliminate",
    doSub: "Important & urgent — crises, deadlines, problems",
    scheduleSub: "Important, not urgent — planning, growth, health",
    delegateSub: "Urgent, not important — interruptions, calls, errands",
    eliminateSub: "Not important, not urgent — time wasters",
    axisImportant: "Important →",
    axisUrgent: "Urgent →",

    addTask: "Add task",
    newTask: "New task",
    editTask: "Edit task",
    title: "Task title",
    notes: "Notes",
    dueDate: "Due date",
    recurrence: "Repeat",
    none: "None",
    daily: "Daily",
    weekly: "Weekly",
    monthly: "Monthly",
    quadrant: "Quadrant",
    save: "Save",
    cancel: "Cancel",
    delete: "Delete",
    done: "Done",
    completedTasks: "Completed tasks",
    showCompleted: "Show completed",
    hideCompleted: "Hide completed",
    noTasks: "No tasks yet — add your first one!",
    dragHint: "Drag tasks between quadrants",
    search: "Search tasks…",
    filterAll: "All",
    filterToday: "Today",
    filterWeek: "This week",
    filterOverdue: "Overdue",

    aiSuggest: "Suggest quadrant (AI)",
    aiThinking: "Analyzing the task…",
    aiSource: "Suggested by AI (Gemini)",
    aiSourceLocal: "Suggested by local engine",
    applySuggestion: "Apply suggestion",

    pomodoro: "Pomodoro",
    start: "Start",
    pause: "Pause",
    resume: "Resume",
    reset: "Reset",
    work: "Focus",
    break: "Break",
    pomoDone: "Session complete! {minutes} minutes done.",
    pomoLogged: "Session saved ({minutes} min)",
    focusOn: "Working on: {task}",
    minutesShort: "min",

    statsTitle: "Your statistics",
    tasksInQuadrants: "Open tasks by quadrant",
    completedThisWeek: "Completed this week",
    pomodorosThisWeek: "Pomodoros this week",
    pomodorosTotal: "Total focus minutes",
    completedByQuadrant: "Completed by quadrant",
    balanceHint: "Tip: most of your time should go to the “Schedule” quadrant — it’s an investment in the future.",

    export: "Export JSON",
    import: "Import JSON",
    imported: "Imported {n} tasks",
    importFailed: "Import failed — check the file.",
    theme: "Theme",
    light: "Light",
    dark: "Dark",
    lang: "Language",
    overdue: "Overdue",
    today: "Today",
    tomorrow: "Tomorrow",
    inDays: "in {n} days",
    repeatDaily: "daily",
    repeatWeekly: "weekly",
    repeatMonthly: "monthly",
    confirmDelete: "Delete this task?",
  },
} as const;

export type TKey = keyof (typeof dict)["pl"];

interface I18nCtx {
  lang: Lang;
  setLang: (l: Lang) => void;
  t: (key: TKey, vars?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nCtx | null>(null);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(() => (localStorage.getItem("em_lang") as Lang) || "pl");

  useEffect(() => {
    localStorage.setItem("em_lang", lang);
    document.documentElement.lang = lang;
  }, [lang]);

  const value: I18nCtx = {
    lang,
    setLang: setLangState,
    t: (key, vars) => {
      let s: string = dict[lang][key] ?? dict.pl[key] ?? key;
      if (vars) for (const [k, v] of Object.entries(vars)) s = s.replace(`{${k}}`, String(v));
      return s;
    },
  };
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nCtx {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within I18nProvider");
  return ctx;
}
