export type Quadrant = "do" | "schedule" | "delegate" | "eliminate";
export type Recurrence = "none" | "daily" | "weekly" | "monthly";

export interface Task {
  id: number;
  title: string;
  notes: string | null;
  quadrant: Quadrant;
  due_date: string | null;
  recurrence: Recurrence;
  position: number;
  done: number;
  completed_at: number | null;
  created_at: number;
}

export interface Suggestion {
  quadrant: Quadrant;
  confidence: number;
  reasons: string[];
  source: "gemini" | "heuristic";
}

export interface Stats {
  openByQuadrant: Record<string, number>;
  doneThisWeek: number;
  doneByQuadrant: Record<string, number>;
  pomodorosThisWeekMinutes: number;
  pomodorosThisWeekCount: number;
  pomodorosTotalMinutes: number;
}
