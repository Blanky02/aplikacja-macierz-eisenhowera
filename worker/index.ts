import { Env, TaskRow } from "./env";
import { initDb } from "./db";
import { hashPassword, verifyPassword, signToken, verifyToken } from "./auth";
import { suggestQuadrant } from "./ai";

const QUADRANTS = ["do", "schedule", "delegate", "eliminate"] as const;
const RECURRENCES = ["none", "daily", "weekly", "monthly"] as const;

const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });

const err = (key: string, status: number) => json({ error: key }, status);

function readBody(req: Request): Promise<Record<string, unknown>> {
  return req.json().catch(() => ({}));
}

function str(v: unknown, max = 300): string | null {
  if (typeof v !== "string") return null;
  const s = v.trim().slice(0, max);
  return s.length ? s : null;
}

function dateStr(v: unknown): string | null {
  if (typeof v !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  return v;
}

async function authUser(req: Request, env: Env): Promise<{ id: number; email: string; name: string } | null> {
  const header = req.headers.get("Authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) return null;
  const payload = await verifyToken(token, env.JWT_SECRET);
  if (!payload) return null;
  const user = await env.DB.prepare("SELECT id, email, name FROM users WHERE id = ?")
    .bind(payload.sub)
    .first<{ id: number; email: string; name: string | null }>();
  if (!user) return null;
  return { id: user.id, email: user.email, name: user.name ?? user.email };
}

function advanceDueDate(due: string, recurrence: string): string | null {
  if (recurrence === "none" || !due) return null;
  const d = new Date(due + "T12:00:00");
  if (recurrence === "daily") d.setDate(d.getDate() + 1);
  else if (recurrence === "weekly") d.setDate(d.getDate() + 7);
  else if (recurrence === "monthly") d.setMonth(d.getMonth() + 1);
  return d.toISOString().slice(0, 10);
}

async function handleApi(path: string, req: Request, env: Env): Promise<Response> {
  const route = (method: string, pattern: string) => {
    const pathParts = path.split("?")[0].split("/").filter(Boolean);
    const patParts = pattern.split("/").filter(Boolean);
    if (req.method !== method || pathParts.length !== patParts.length) return null;
    const params: Record<string, string> = {};
    for (let i = 0; i < patParts.length; i++) {
      if (patParts[i].startsWith(":")) params[patParts[i].slice(1)] = pathParts[i];
      else if (patParts[i] !== pathParts[i]) return null;
    }
    return params;
  };

  // ---- Auth: rejestracja ----
  if (route("POST", "/api/register")) {
    const body = await readBody(req);
    const email = str(body.email, 120)?.toLowerCase();
    const password = typeof body.password === "string" ? body.password : "";
    const name = str(body.name, 80);
    if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) return err("invalid_email", 400);
    if (password.length < 6) return err("password_too_short", 400);
    const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
    if (existing) return err("email_taken", 409);
    const hash = await hashPassword(password);
    const res = await env.DB.prepare("INSERT INTO users (email, password_hash, name, created_at) VALUES (?, ?, ?, ?)")
      .bind(email, hash, name, Date.now())
      .run();
    const id = res.meta.last_row_id as number;
    const token = await signToken({ sub: id, email }, env.JWT_SECRET);
    return json({ token, user: { id, email, name: name ?? email } }, 201);
  }

  // ---- Auth: logowanie ----
  if (route("POST", "/api/login")) {
    const body = await readBody(req);
    const email = str(body.email, 120)?.toLowerCase();
    const password = typeof body.password === "string" ? body.password : "";
    if (!email || !password) return err("invalid_credentials", 401);
    const user = await env.DB.prepare("SELECT id, email, password_hash, name FROM users WHERE email = ?")
      .bind(email)
      .first<{ id: number; email: string; password_hash: string; name: string | null }>();
    if (!user || !(await verifyPassword(password, user.password_hash))) return err("invalid_credentials", 401);
    const token = await signToken({ sub: user.id, email: user.email }, env.JWT_SECRET);
    return json({ token, user: { id: user.id, email: user.email, name: user.name ?? user.email } });
  }

  // ---- AI: sugestia kwadrantu ----
  if (route("POST", "/api/ai/suggest")) {
    const user = await authUser(req, env);
    if (!user) return err("unauthorized", 401);
    const body = await readBody(req);
    const title = str(body.title, 300);
    if (!title) return err("title_required", 400);
    const notes = str(body.notes, 2000);
    const due = dateStr(body.due_date);
    const lang = body.lang === "en" ? "en" : "pl";
    const suggestion = await suggestQuadrant(title, notes ?? "", due, lang, env);
    return json(suggestion);
  }

  // Wszystko poniżej wymaga autoryzacji
  const user = await authUser(req, env);
  if (!user) return err("unauthorized", 401);

  // ---- Lista zadań ----
  if (route("GET", "/api/tasks")) {
    const tasks = await env.DB.prepare(
      "SELECT id, title, notes, quadrant, due_date, recurrence, position, done, created_at, completed_at FROM tasks WHERE user_id = ? ORDER BY done ASC, quadrant ASC, position ASC"
    )
      .bind(user.id)
      .all<TaskRow>();
    return json(tasks.results);
  }

  // ---- Tworzenie zadania ----
  if (route("POST", "/api/tasks")) {
    const body = await readBody(req);
    const title = str(body.title, 300);
    if (!title) return err("title_required", 400);
    const quadrant = QUADRANTS.includes(body.quadrant as (typeof QUADRANTS)[number])
      ? (body.quadrant as (typeof QUADRANTS)[number])
      : "schedule";
    const due = dateStr(body.due_date);
    const recurrence = RECURRENCES.includes(body.recurrence as (typeof RECURRENCES)[number])
      ? (body.recurrence as string)
      : "none";
    const notes = str(body.notes, 2000);
    const maxPos = await env.DB.prepare("SELECT COALESCE(MAX(position), -1) AS m FROM tasks WHERE user_id = ? AND quadrant = ?")
      .bind(user.id, quadrant)
      .first<{ m: number }>();
    const res = await env.DB.prepare(
      "INSERT INTO tasks (user_id, title, notes, quadrant, due_date, recurrence, position, done, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)"
    )
      .bind(user.id, title, notes, quadrant, due, recurrence, (maxPos?.m ?? -1) + 1, Date.now())
      .run();
    const task = await env.DB.prepare("SELECT id, title, notes, quadrant, due_date, recurrence, position, done, created_at, completed_at FROM tasks WHERE id = ?")
      .bind(res.meta.last_row_id)
      .first<TaskRow>();
    return json(task, 201);
  }

  // ---- Statystyki ----
  if (route("GET", "/api/stats")) {
    const now = Date.now();
    const weekAgo = now - 7 * 86_400_000;
    const [open, doneWeek, byQ, pomoWeek, totalPomo] = await Promise.all([
      env.DB.prepare("SELECT quadrant, COUNT(*) AS n FROM tasks WHERE user_id = ? AND done = 0 GROUP BY quadrant").bind(user.id).all<{ quadrant: string; n: number }>(),
      env.DB.prepare("SELECT COUNT(*) AS n FROM tasks WHERE user_id = ? AND done = 1 AND completed_at >= ?").bind(user.id, weekAgo).first<{ n: number }>(),
      env.DB.prepare("SELECT quadrant, COUNT(*) AS n FROM tasks WHERE user_id = ? AND done = 1 GROUP BY quadrant").bind(user.id).all<{ quadrant: string; n: number }>(),
      env.DB.prepare("SELECT COALESCE(SUM(minutes), 0) AS m, COUNT(*) AS n FROM pomodoros WHERE user_id = ? AND created_at >= ?").bind(user.id, weekAgo).first<{ m: number; n: number }>(),
      env.DB.prepare("SELECT COALESCE(SUM(minutes), 0) AS m FROM pomodoros WHERE user_id = ?").bind(user.id).first<{ m: number }>(),
    ]);
    return json({
      openByQuadrant: Object.fromEntries(open.results.map((r) => [r.quadrant, r.n])),
      doneThisWeek: doneWeek?.n ?? 0,
      doneByQuadrant: Object.fromEntries(byQ.results.map((r) => [r.quadrant, r.n])),
      pomodorosThisWeekMinutes: pomoWeek?.m ?? 0,
      pomodorosThisWeekCount: pomoWeek?.n ?? 0,
      pomodorosTotalMinutes: totalPomo?.m ?? 0,
    });
  }

  // ---- Eksport ----
  if (route("GET", "/api/export")) {
    const tasks = await env.DB.prepare("SELECT title, notes, quadrant, due_date, recurrence, position, done, created_at, completed_at FROM tasks WHERE user_id = ? ORDER BY id ASC")
      .bind(user.id)
      .all<TaskRow>();
    return json({ version: 1, exportedAt: new Date().toISOString(), tasks: tasks.results });
  }

  // ---- Import ----
  if (route("POST", "/api/import")) {
    const body = await readBody(req);
    const tasks = Array.isArray(body.tasks) ? (body.tasks as Record<string, unknown>[]) : [];
    if (!tasks.length) return err("no_tasks", 400);
    let imported = 0;
    const stmts = tasks.slice(0, 500).map((t) => {
      const title = str(t.title, 300);
      if (!title) return null;
      const quadrant = QUADRANTS.includes(t.quadrant as (typeof QUADRANTS)[number]) ? (t.quadrant as string) : "schedule";
      imported++;
      return env.DB.prepare(
        "INSERT INTO tasks (user_id, title, notes, quadrant, due_date, recurrence, position, done, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      ).bind(
        user.id,
        title,
        str(t.notes, 2000),
        quadrant,
        dateStr(t.due_date),
        RECURRENCES.includes(t.recurrence as (typeof RECURRENCES)[number]) ? (t.recurrence as string) : "none",
        typeof t.position === "number" ? t.position : imported,
        t.done ? 1 : 0,
        Date.now(),
        t.done ? Date.now() : null
      );
    }).filter(Boolean);
    if (stmts.length) await env.DB.batch(stmts as D1PreparedStatement[]);
    return json({ imported });
  }

  // ---- Pomodoro: zapis sesji ----
  if (route("POST", "/api/pomodoros")) {
    const body = await readBody(req);
    const minutes = Math.min(180, Math.max(1, Number(body.minutes) || 25));
    const taskId = typeof body.task_id === "number" ? body.task_id : null;
    await env.DB.prepare("INSERT INTO pomodoros (user_id, task_id, minutes, created_at) VALUES (?, ?, ?, ?)")
      .bind(user.id, taskId, minutes, Date.now())
      .run();
    return json({ ok: true }, 201);
  }

  // ---- Zadanie: aktualizacja ----
  const updateMatch = route("PATCH", "/api/tasks/:id");
  if (updateMatch) {
    const id = Number(updateMatch.id);
    const existing = await env.DB.prepare("SELECT * FROM tasks WHERE id = ? AND user_id = ?").bind(id, user.id).first<TaskRow>();
    if (!existing) return err("not_found", 404);
    const body = await readBody(req);

    const title = body.title !== undefined ? str(body.title, 300) : existing.title;
    if (!title) return err("title_required", 400);
    const notes = body.notes !== undefined ? str(body.notes, 2000) : existing.notes;
    const quadrant = body.quadrant !== undefined && QUADRANTS.includes(body.quadrant as (typeof QUADRANTS)[number])
      ? (body.quadrant as string)
      : existing.quadrant;
    const due = body.due_date !== undefined ? dateStr(body.due_date) : existing.due_date;
    const recurrence = body.recurrence !== undefined && RECURRENCES.includes(body.recurrence as (typeof RECURRENCES)[number])
      ? (body.recurrence as string)
      : existing.recurrence;
    const position = typeof body.position === "number" ? body.position : existing.position;

    let done = existing.done;
    let completedAt = existing.completed_at;
    if (body.done !== undefined) {
      done = body.done ? 1 : 0;
      completedAt = done ? Date.now() : null;
    }

    // Powtarzające się zadanie ukończone → archiwizujemy i tworzymy następną instancję
    if (done === 1 && existing.done === 0 && existing.recurrence !== "none" && existing.due_date) {
      const nextDue = advanceDueDate(existing.due_date, existing.recurrence);
      await env.DB.prepare(
        "INSERT INTO tasks (user_id, title, notes, quadrant, due_date, recurrence, position, done, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)"
      )
        .bind(user.id, existing.title, existing.notes, existing.quadrant, nextDue, existing.recurrence, existing.position, Date.now())
        .run();
    }

    await env.DB.prepare(
      "UPDATE tasks SET title = ?, notes = ?, quadrant = ?, due_date = ?, recurrence = ?, position = ?, done = ?, completed_at = ? WHERE id = ?"
    )
      .bind(title, notes, quadrant, due, recurrence, position, done, completedAt, id)
      .run();
    const task = await env.DB.prepare("SELECT id, title, notes, quadrant, due_date, recurrence, position, done, created_at, completed_at FROM tasks WHERE id = ?")
      .bind(id)
      .first<TaskRow>();
    return json(task);
  }

  // ---- Zadanie: przeniesienie / zmiana kolejności (drag & drop) ----
  const reorderMatch = route("POST", "/api/tasks/reorder");
  if (reorderMatch) {
    const body = await readBody(req);
    const order = Array.isArray(body.order) ? (body.order as unknown[]) : [];
    if (!order.length) return err("empty_order", 400);
    const ids = order.map(Number).filter((n) => Number.isInteger(n) && n > 0);
    if (!ids.length) return err("empty_order", 400);
    const stmts = ids.map((id, idx) =>
      env.DB.prepare("UPDATE tasks SET position = ? WHERE id = ? AND user_id = ?").bind(idx, id, user.id)
    );
    await env.DB.batch(stmts);
    return json({ ok: true });
  }

  // ---- Zadanie: usuwanie ----
  const deleteMatch = route("DELETE", "/api/tasks/:id");
  if (deleteMatch) {
    const id = Number(deleteMatch.id);
    await env.DB.prepare("DELETE FROM tasks WHERE id = ? AND user_id = ?").bind(id, user.id).run();
    return json({ ok: true });
  }

  return err("not_found", 404);
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url);
    if (url.pathname.startsWith("/api/")) {
      try {
        await initDb(env.DB);
        return await handleApi(url.pathname, req, env);
      } catch (e) {
        console.error("API error:", e);
        return err("server_error", 500);
      }
    }
    // Statyczne zasoby / SPA
    return env.ASSETS.fetch(req);
  },
};
