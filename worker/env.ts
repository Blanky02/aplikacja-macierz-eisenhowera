// Typy środowiska Cloudflare Worker
export interface Env {
  DB: D1Database;
  ASSETS: Fetcher;
  GEMINI_API_KEY?: string;
  GEMINI_MODEL?: string;
  JWT_SECRET: string;
}

export type Quadrant = "do" | "schedule" | "delegate" | "eliminate";

export interface TaskRow {
  id: number;
  user_id: number;
  title: string;
  notes: string | null;
  quadrant: Quadrant;
  due_date: string | null;
  recurrence: string;
  position: number;
  done: number;
  created_at: number;
  completed_at: number | null;
}
