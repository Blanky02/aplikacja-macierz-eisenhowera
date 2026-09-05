import { useEffect, useState } from "react";
import type { Quadrant, Stats } from "../types";
import { api } from "../api";
import { useI18n, type TKey } from "../i18n";

const QUADRANTS: Quadrant[] = ["do", "schedule", "delegate", "eliminate"];
const Q_KEYS: Record<Quadrant, TKey> = {
  do: "importantUrgent",
  schedule: "importantNotUrgent",
  delegate: "notImportantUrgent",
  eliminate: "notImportantNotUrgent",
};
const Q_COLORS: Record<Quadrant, string> = {
  do: "var(--do)",
  schedule: "var(--schedule)",
  delegate: "var(--delegate)",
  eliminate: "var(--eliminate)",
};

export function StatsPage() {
  const { t } = useI18n();
  const [stats, setStats] = useState<Stats | null>(null);

  useEffect(() => {
    api.stats().then(setStats).catch(() => {});
  }, []);

  const openTotal = stats ? QUADRANTS.reduce((sum, q) => sum + (stats.openByQuadrant[q] ?? 0), 0) : 0;
  const doneTotal = stats ? QUADRANTS.reduce((sum, q) => sum + (stats.doneByQuadrant[q] ?? 0), 0) : 0;

  return (
    <>
      <div className="page-head">
        <h1>{t("statsTitle")}</h1>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">{t("completedThisWeek")}</div>
          <div className="stat-value">{stats?.doneThisWeek ?? "—"}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">{t("pomodorosThisWeek")}</div>
          <div className="stat-value">{stats?.pomodorosThisWeekCount ?? "—"}</div>
          <div className="stat-sub">{stats?.pomodorosThisWeekMinutes ?? 0} {t("minutesShort")} 🍅</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">{t("pomodorosTotal")}</div>
          <div className="stat-value">{stats?.pomodorosTotalMinutes ?? "—"}</div>
          <div className="stat-sub">{t("minutesShort")}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">{t("tasksInQuadrants")}</div>
          <div className="stat-value">{openTotal}</div>
        </div>
      </div>

      <div className="bars-card">
        <h3>{t("tasksInQuadrants")}</h3>
        {QUADRANTS.map((q) => {
          const n = stats?.openByQuadrant[q] ?? 0;
          const pct = openTotal ? Math.round((n / openTotal) * 100) : 0;
          return (
            <div className="bar-row" key={q}>
              <span className="bar-label">{t(Q_KEYS[q])}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${pct}%`, background: Q_COLORS[q] }} />
              </div>
              <span className="bar-num">{n}</span>
            </div>
          );
        })}
      </div>

      <div className="bars-card" style={{ marginTop: 16 }}>
        <h3>{t("completedByQuadrant")}</h3>
        {QUADRANTS.map((q) => {
          const n = stats?.doneByQuadrant[q] ?? 0;
          const pct = doneTotal ? Math.round((n / doneTotal) * 100) : 0;
          return (
            <div className="bar-row" key={q}>
              <span className="bar-label">{t(Q_KEYS[q])}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${pct}%`, background: Q_COLORS[q], opacity: 0.7 }} />
              </div>
              <span className="bar-num">{n}</span>
            </div>
          );
        })}
        <div className="balance-hint">💡 {t("balanceHint")}</div>
      </div>
    </>
  );
}
