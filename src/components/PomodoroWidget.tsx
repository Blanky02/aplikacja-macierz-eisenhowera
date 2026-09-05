import { useEffect, useRef, useState } from "react";
import { api } from "../api";
import { useI18n } from "../i18n";
import type { Task } from "../types";

const WORK = 25 * 60;
const BREAK = 5 * 60;

function beep() {
  try {
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new Ctx();
    [0, 0.25, 0.5].forEach((delay, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = i === 2 ? 880 : 660;
      gain.gain.setValueAtTime(0.001, ctx.currentTime + delay);
      gain.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + delay + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + delay + 0.2);
      osc.start(ctx.currentTime + delay);
      osc.stop(ctx.currentTime + delay + 0.25);
    });
  } catch {
    /* dźwięk opcjonalny */
  }
}

export function PomodoroWidget({ activeTask }: { activeTask: Task | null }) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);
  const [phase, setPhase] = useState<"work" | "break">("work");
  const [left, setLeft] = useState(WORK);
  const [running, setRunning] = useState(false);
  const intervalRef = useRef<number | null>(null);
  const loggedRef = useRef(false);

  useEffect(() => {
    if (running) {
      intervalRef.current = window.setInterval(() => setLeft((s) => s - 1), 1000);
    } else if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [running]);

  useEffect(() => {
    if (left > 0) return;
    beep();
    setRunning(false);
    if (phase === "work" && !loggedRef.current) {
      loggedRef.current = true;
      api.logPomodoro(25, activeTask?.id ?? null).catch(() => {});
    }
    setPhase((p) => (p === "work" ? "break" : "work"));
    setLeft(phase === "work" ? BREAK : WORK);
  }, [left, phase, activeTask]);

  const reset = () => {
    setRunning(false);
    setPhase("work");
    setLeft(WORK);
    loggedRef.current = false;
  };

  const mm = String(Math.floor(left / 60)).padStart(2, "0");
  const ss = String(left % 60).padStart(2, "0");

  return (
    <div className="pomo-float">
      {open && (
        <div className="pomo-panel">
          <h3>🍅 {t("pomodoro")}</h3>
          {activeTask && <div className="pomo-task">{t("focusOn", { task: activeTask.title })}</div>}
          <div className="pomo-time">{mm}:{ss}</div>
          <div className="pomo-phase">{phase === "work" ? t("work") : t("break")}</div>
          <div className="pomo-controls">
            <button className="btn btn-sm" onClick={() => setRunning((r) => !r)}>
              {running ? t("pause") : left === (phase === "work" ? WORK : BREAK) ? t("start") : t("resume")}
            </button>
            <button className="btn btn-ghost btn-sm" onClick={reset}>{t("reset")}</button>
          </div>
        </div>
      )}
      <button className="pomo-btn" title={t("pomodoro")} onClick={() => setOpen((o) => !o)}>
        🍅
      </button>
    </div>
  );
}
