import { useEffect, useState } from "react";
import type { Quadrant, Recurrence, Suggestion, Task } from "../types";
import { api } from "../api";
import { useI18n, type TKey } from "../i18n";

interface Props {
  task: Task | null; // null = tworzenie nowego
  initialQuadrant?: Quadrant;
  onClose: () => void;
  onSaved: () => void;
}

const Q_KEYS: Record<Quadrant, TKey> = {
  do: "importantUrgent",
  schedule: "importantNotUrgent",
  delegate: "notImportantUrgent",
  eliminate: "notImportantNotUrgent",
};

export function TaskModal({ task, initialQuadrant, onClose, onSaved }: Props) {
  const { t, lang } = useI18n();
  const [title, setTitle] = useState(task?.title ?? "");
  const [notes, setNotes] = useState(task?.notes ?? "");
  const [quadrant, setQuadrant] = useState<Quadrant>(task?.quadrant ?? initialQuadrant ?? "schedule");
  const [dueDate, setDueDate] = useState<string>(task?.due_date ?? "");
  const [recurrence, setRecurrence] = useState<Recurrence>(task?.recurrence ?? "none");
  const [suggestion, setSuggestion] = useState<Suggestion | null>(null);
  const [aiBusy, setAiBusy] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const askAi = async () => {
    if (!title.trim()) return;
    setAiBusy(true);
    setSuggestion(null);
    try {
      const s = await api.suggest({
        title: title.trim(),
        notes: notes.trim() || undefined,
        due_date: dueDate || null,
        lang,
      });
      setSuggestion(s);
    } catch {
      /* ignorujemy — użytkownik może spróbować ponownie */
    } finally {
      setAiBusy(false);
    }
  };

  const save = async () => {
    if (!title.trim()) return;
    setSaving(true);
    try {
      const payload = {
        title: title.trim(),
        notes: notes.trim() || null,
        quadrant,
        due_date: dueDate || null,
        recurrence,
      };
      if (task) await api.updateTask(task.id, payload);
      else await api.createTask(payload);
      onSaved();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <h2>{task ? t("editTask") : t("newTask")}</h2>

        <div className="field">
          <label>{t("title")}</label>
          <input
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && save()}
            placeholder="…"
          />
        </div>

        <div className="field">
          <label>{t("notes")}</label>
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>

        <div className="modal-row">
          <div className="field">
            <label>{t("dueDate")}</label>
            <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>
          <div className="field">
            <label>{t("recurrence")}</label>
            <select value={recurrence} onChange={(e) => setRecurrence(e.target.value as Recurrence)}>
              <option value="none">{t("none")}</option>
              <option value="daily">{t("daily")}</option>
              <option value="weekly">{t("weekly")}</option>
              <option value="monthly">{t("monthly")}</option>
            </select>
          </div>
        </div>

        <div className="field">
          <label>{t("quadrant")}</label>
          <div className="chips">
            {(["do", "schedule", "delegate", "eliminate"] as Quadrant[]).map((q) => (
              <button
                key={q}
                type="button"
                className={`chip ${quadrant === q ? "active" : ""}`}
                onClick={() => setQuadrant(q)}
              >
                {t(Q_KEYS[q])}
              </button>
            ))}
          </div>
        </div>

        <button className="btn btn-ghost btn-sm" onClick={askAi} disabled={aiBusy || !title.trim()}>
          {aiBusy ? <span className="ai-spinner" /> : "✨"} {t("aiSuggest")}
        </button>

        {suggestion && (
          <div className="ai-box">
            <div className="ai-head">
              ✨ {t(Q_KEYS[suggestion.quadrant])}
              <span className="ai-confidence">{suggestion.confidence}%</span>
            </div>
            <ul className="ai-reasons">
              {suggestion.reasons.map((r, i) => <li key={i}>{r}</li>)}
            </ul>
            <div className="ai-source">
              {suggestion.source === "gemini" ? t("aiSource") : t("aiSourceLocal")}
            </div>
            <div style={{ marginTop: 10 }}>
              <button className="btn btn-sm" onClick={() => setQuadrant(suggestion.quadrant)}>
                {t("applySuggestion")}
              </button>
            </div>
          </div>
        )}

        <div className="modal-actions">
          <button className="btn btn-ghost" onClick={onClose}>{t("cancel")}</button>
          <button className="btn" onClick={save} disabled={saving || !title.trim()}>
            {saving ? "…" : t("save")}
          </button>
        </div>
      </div>
    </div>
  );
}
