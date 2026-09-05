import type { Task, Suggestion, Stats, Quadrant, Recurrence } from "./types";

const TOKEN_KEY = "em_token";
const USER_KEY = "em_user";

export interface AuthUser {
  id: number;
  email: string;
  name: string;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
export function setSession(token: string, user: AuthUser): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}
export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
export function getUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(path, { ...options, headers });
  const data = (await res.json().catch(() => ({}))) as Record<string, unknown>;
  if (res.status === 401) clearSession();
  if (!res.ok) throw new Error((data.error as string) || "network_error");
  return data as T;
}

export interface TaskInput {
  title: string;
  notes?: string | null;
  quadrant?: Quadrant;
  due_date?: string | null;
  recurrence?: Recurrence;
}

export const api = {
  login: (email: string, password: string) =>
    request<{ token: string; user: AuthUser }>("/api/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),
  register: (email: string, password: string, name?: string) =>
    request<{ token: string; user: AuthUser }>("/api/register", {
      method: "POST",
      body: JSON.stringify({ email, password, name }),
    }),
  tasks: () => request<Task[]>("/api/tasks"),
  createTask: (payload: TaskInput) =>
    request<Task>("/api/tasks", { method: "POST", body: JSON.stringify(payload) }),
  updateTask: (id: number, patch: Partial<TaskInput> & { done?: boolean; position?: number }) =>
    request<Task>(`/api/tasks/${id}`, { method: "PATCH", body: JSON.stringify(patch) }),
  deleteTask: (id: number) =>
    request<{ ok: true }>(`/api/tasks/${id}`, { method: "DELETE" }),
  reorder: (order: number[]) =>
    request<{ ok: true }>("/api/tasks/reorder", { method: "POST", body: JSON.stringify({ order }) }),
  suggest: (payload: { title: string; notes?: string; due_date?: string | null; lang: "pl" | "en" }) =>
    request<Suggestion>("/api/ai/suggest", { method: "POST", body: JSON.stringify(payload) }),
  stats: () => request<Stats>("/api/stats"),
  logPomodoro: (minutes: number, taskId: number | null) =>
    request<{ ok: true }>("/api/pomodoros", { method: "POST", body: JSON.stringify({ minutes, task_id: taskId }) }),
  exportData: () => request<{ version: number; tasks: Task[] }>("/api/export"),
  importData: (tasks: unknown[]) =>
    request<{ imported: number }>("/api/import", { method: "POST", body: JSON.stringify({ tasks }) }),
};
