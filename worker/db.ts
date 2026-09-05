import { hashPassword } from "./auth";

const SCHEMA_STATEMENTS = [
  `CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name TEXT,
    created_at INTEGER NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    notes TEXT,
    quadrant TEXT NOT NULL DEFAULT 'schedule',
    due_date TEXT,
    recurrence TEXT NOT NULL DEFAULT 'none',
    position INTEGER NOT NULL DEFAULT 0,
    done INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    completed_at INTEGER
  )`,
  `CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id)`,
  `CREATE TABLE IF NOT EXISTS pomodoros (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id INTEGER REFERENCES tasks(id) ON DELETE SET NULL,
    minutes INTEGER NOT NULL,
    created_at INTEGER NOT NULL
  )`,
  `CREATE INDEX IF NOT EXISTS idx_pomo_user ON pomodoros(user_id)`,
];

interface DemoTask {
  title: string;
  quadrant: string;
  due: number | null;
  pos: number;
}

const DEMO_TASKS: DemoTask[] = [
  { title: "Awaria serwera produkcyjnego — przywrócić działanie", quadrant: "do", due: 0, pos: 0 },
  { title: "Raport dla klienta do końca dnia", quadrant: "do", due: 0, pos: 1 },
  { title: "Zaplanować strategię na następny kwartał", quadrant: "schedule", due: 7, pos: 0 },
  { title: "Nauka TypeScript — moduł 3", quadrant: "schedule", due: 3, pos: 1 },
  { title: "Ćwiczenia fizyczne — 30 minut", quadrant: "schedule", due: 1, pos: 2 },
  { title: "Telefon od współpracownika w drobnej sprawie", quadrant: "delegate", due: null, pos: 0 },
  { title: "Maile z prośbą o potwierdzenie spotkań", quadrant: "delegate", due: null, pos: 1 },
  { title: "Przeglądanie mediów społecznościowych", quadrant: "eliminate", due: null, pos: 0 },
  { title: "Porządkowanie starych plików na pulpicie", quadrant: "eliminate", due: null, pos: 1 },
];

let dbReady: Promise<void> | null = null;

// Inicjalizacja raz na izolat Workera (nie przy każdym żądaniu)
export function initDbOnce(db: D1Database): Promise<void> {
  if (!dbReady) {
    dbReady = initDb(db).catch((e) => {
      dbReady = null; // pozwól na ponowną próbę przy kolejnym żądaniu
      throw e;
    });
  }
  return dbReady;
}

async function initDb(db: D1Database): Promise<void> {
  await db.batch(SCHEMA_STATEMENTS.map((sql) => db.prepare(sql)));

  const existing = await db.prepare("SELECT id FROM users WHERE email = ?").bind("demo@matrix.app").first<{ id: number }>();
  if (existing) return;

  const hash = await hashPassword("demo1234");
  const res = await db
    .prepare("INSERT INTO users (email, password_hash, name, created_at) VALUES (?, ?, ?, ?)")
    .bind("demo@matrix.app", hash, "Demo", Date.now())
    .run();
  const userId = res.meta.last_row_id as number;
  const now = Date.now();
  const day = 86_400_000;

  const insert = db.prepare(
    "INSERT INTO tasks (user_id, title, notes, quadrant, due_date, recurrence, position, done, created_at, completed_at) VALUES (?, ?, NULL, ?, ?, 'none', ?, 0, ?, NULL)"
  );
  await db.batch(
    DEMO_TASKS.map((t) => {
      const due = t.due === null ? null : new Date(now + t.due * day).toISOString().slice(0, 10);
      return insert.bind(userId, t.title, t.quadrant, due, t.pos, now);
    })
  );
}
