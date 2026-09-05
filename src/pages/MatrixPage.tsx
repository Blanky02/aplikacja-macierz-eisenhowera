import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import type { Quadrant, Task } from "../types";
import { api } from "../api";
import { useI18n, type TKey } from "../i18n";
import { QuadrantColumn } from "../components/QuadrantColumn";
import { TaskModal } from "../components/TaskModal";
import { PomodoroWidget } from "../components/PomodoroWidget";
import { TaskCard } from "../components/TaskCard";

const QUADRANTS: Quadrant[] = ["do", "schedule", "delegate", "eliminate"];

type Filter = "all" | "today" | "week" | "overdue";

function dayDiff(due: string): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const d = new Date(due + "T00:00:00");
  return Math.round((d.getTime() - today.getTime()) / 86_400_000);
}

export function MatrixPage() {
  const { t, lang } = useI18n();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<{ task: Task | null; quadrant?: Quadrant } | null>(null);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<Filter>("all");
  const [showDone, setShowDone] = useState(false);
  const [activeDrag, setActiveDrag] = useState<Task | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const dragStartQuadrant = useRef<Quadrant | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast(null), 2600);
  }, []);

  const load = useCallback(async () => {
    try {
      const data = await api.tasks();
      setTasks(data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } })
  );

  const matchesFilter = (task: Task): boolean => {
    if (filter === "all") return true;
    if (!task.due_date) return false;
    const diff = dayDiff(task.due_date);
    if (filter === "overdue") return diff < 0;
    if (filter === "today") return diff <= 1;
    if (filter === "week") return diff <= 7;
    return true;
  };

  const openTasks = useMemo(
    () =>
      tasks
        .filter((t) => t.done === 0)
        .filter((t) => matchesFilter(t))
        .filter((t) =>
          search.trim()
            ? (t.title + " " + (t.notes ?? "")).toLowerCase().includes(search.trim().toLowerCase())
            : true
        ),
    [tasks, filter, search] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const doneTasks = useMemo(() => tasks.filter((t) => t.done === 1), [tasks]);

  const byQuadrant = useCallback(
    (q: Quadrant) => openTasks.filter((t) => t.quadrant === q).sort((a, b) => a.position - b.position),
    [openTasks]
  );

  const dragDisabled = filter !== "all" || search.trim().length > 0;

  // ---------- DnD ----------
  const onDragStart = (e: DragStartEvent) => {
    const task = e.active.data.current?.task as Task | undefined;
    if (task) {
      setActiveDrag(task);
      dragStartQuadrant.current = task.quadrant;
    }
  };

  const onDragOver = (e: DragOverEvent) => {
    const { active, over } = e;
    if (!over) return;
    const activeTask = tasks.find((t) => t.id === active.id);
    if (!activeTask) return;

    const overId = over.id as string;
    let targetQuadrant: Quadrant | null = null;
    if (overId.startsWith("q-")) {
      targetQuadrant = overId.slice(2) as Quadrant;
    } else {
      const overTask = tasks.find((t) => t.id === over.id);
      if (overTask) targetQuadrant = overTask.quadrant;
    }
    if (!targetQuadrant || targetQuadrant === activeTask.quadrant) return;

    setTasks((prev) =>
      prev.map((t) => (t.id === activeTask.id ? { ...t, quadrant: targetQuadrant! } : t))
    );
  };

  const onDragEnd = async (e: DragEndEvent) => {
    const { active, over } = e;
    setActiveDrag(null);
    if (!over) {
      dragStartQuadrant.current = null;
      return;
    }
    const draggedId = active.id as number;
    const dragged = tasks.find((t) => t.id === draggedId);
    const startQ = dragStartQuadrant.current;
    dragStartQuadrant.current = null;
    if (!dragged || !startQ) return;

    // Kolejność wg stanu po przeciągnięciu
    const targetTasks = tasks
      .filter((t) => t.done === 0 && t.quadrant === dragged.quadrant)
      .sort((a, b) => (a.id === draggedId ? 1 : 0) - (b.id === draggedId ? 1 : 0) || a.position - b.position);

    // Poprawiamy pozycje lokalnie
    setTasks((prev) =>
      prev.map((t) => {
        const idx = targetTasks.findIndex((x) => x.id === t.id);
        return idx >= 0 ? { ...t, position: idx } : t;
      })
    );

    try {
      if (startQ !== dragged.quadrant) {
        await api.updateTask(draggedId, { quadrant: dragged.quadrant });
      }
      await api.reorder(targetTasks.map((t) => t.id));
    } catch {
      load();
    }
  };

  // ---------- Operacje na zadaniach ----------
  const toggleDone = async (task: Task) => {
    setTasks((prev) =>
      prev.map((t) =>
        t.id === task.id
          ? { ...t, done: t.done ? 0 : 1, completed_at: t.done ? null : Date.now() }
          : t
      )
    );
    try {
      await api.updateTask(task.id, { done: !task.done });
      load();
    } catch {
      load();
    }
  };

  const removeTask = async (task: Task) => {
    if (!window.confirm(t("confirmDelete"))) return;
    setTasks((prev) => prev.filter((t) => t.id !== task.id));
    try {
      await api.deleteTask(task.id);
      showToast(t("delete") + " ✓");
    } catch {
      load();
    }
  };

  const exportJson = async () => {
    const data = await api.exportData();
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `eisenhower-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const importJson = async (file: File) => {
    try {
      const text = await file.text();
      const data = JSON.parse(text);
      const res = await api.importData(Array.isArray(data.tasks) ? data.tasks : []);
      showToast(t("imported", { n: res.imported }));
      load();
    } catch {
      showToast(t("importFailed"));
    }
  };

  const filters: { id: Filter; key: TKey }[] = [
    { id: "all", key: "filterAll" },
    { id: "today", key: "filterToday" },
    { id: "week", key: "filterWeek" },
    { id: "overdue", key: "filterOverdue" },
  ];

  const focusTask = useMemo(() => byQuadrant("do")[0] ?? null, [byQuadrant]);

  return (
    <>
      <div className="page-head">
        <h1>{t("appName")}</h1>
        <p>{t("tagline")} · {t("dragHint")}</p>
      </div>

      <div className="toolbar">
        <button className="btn" onClick={() => setModal({ task: null })}>＋ {t("addTask")}</button>
        <div className="search-box">
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={t("search")} />
        </div>
        <div className="chips">
          {filters.map((f) => (
            <button key={f.id} className={`chip ${filter === f.id ? "active" : ""}`} onClick={() => setFilter(f.id)}>
              {t(f.key)}
            </button>
          ))}
        </div>
        <button className="icon-btn" title={t("export")} onClick={exportJson}>⬇</button>
        <button className="icon-btn" title={t("import")} onClick={() => fileRef.current?.click()}>⬆</button>
        <input
          ref={fileRef}
          type="file"
          accept="application/json"
          style={{ display: "none" }}
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) importJson(f);
            e.target.value = "";
          }}
        />
      </div>

      {loading ? (
        <div className="quadrant" style={{ minHeight: 200 }}>
          <div className="empty-hint">…</div>
        </div>
      ) : (
        <DndContext
          sensors={sensors}
          onDragStart={onDragStart}
          onDragOver={dragDisabled ? undefined : onDragOver}
          onDragEnd={dragDisabled ? undefined : onDragEnd}
          onDragCancel={() => setActiveDrag(null)}
        >
          <div className="matrix-wrapper">
            <div className="matrix-axes">
              <span className="axis-label-h">{t("axisUrgent")}</span>
            </div>
            <div className="matrix-grid">
              <div className="axis-y">
                <span className="axis-label-v">{t("axisImportant")}</span>
              </div>
              {QUADRANTS.map((q) => (
                <QuadrantColumn
                  key={q}
                  quadrant={q}
                  tasks={byQuadrant(q)}
                  onToggleDone={toggleDone}
                  onEdit={(task) => setModal({ task })}
                  onDelete={removeTask}
                />
              ))}
            </div>
          </div>

          <DragOverlay>
            {activeDrag ? (
              <div className="task-card" style={{ width: 280, boxShadow: "var(--shadow)" }}>
                <div className="task-top">
                  <span className="task-title">{activeDrag.title}</span>
                </div>
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      )}

      <div className="completed-section">
        <button className="completed-toggle" onClick={() => setShowDone((s) => !s)}>
          {showDone ? "▾" : "▸"} {t(showDone ? "hideCompleted" : "showCompleted")} ({doneTasks.length})
        </button>
        {showDone && (
          <SortableContext items={[]} strategy={verticalListSortingStrategy}>
            <div className="completed-list">
              {doneTasks.length === 0 && <div className="empty-hint">{t("completedTasks")} — 0</div>}
              {doneTasks.map((task) => (
                <TaskCard key={task.id} task={task} onToggleDone={toggleDone} onEdit={(t) => setModal({ task: t })} onDelete={removeTask} />
              ))}
            </div>
          </SortableContext>
        )}
      </div>

      {modal && (
        <TaskModal
          task={modal.task}
          initialQuadrant={modal.quadrant}
          onClose={() => setModal(null)}
          onSaved={() => {
            setModal(null);
            load();
          }}
        />
      )}

      <PomodoroWidget activeTask={focusTask} />
      {toast && <div className="toast">{toast}</div>}
    </>
  );
}
