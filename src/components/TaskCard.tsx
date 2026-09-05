import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import type { Task } from "../types";
import { useI18n } from "../i18n";

interface Props {
  task: Task;
  onToggleDone: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
}

function dueInfo(task: Task, t: ReturnType<typeof useI18n>["t"]) {
  if (!task.due_date) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const due = new Date(task.due_date + "T00:00:00");
  const days = Math.round((due.getTime() - today.getTime()) / 86_400_000);
  if (days < 0) return { label: t("overdue"), cls: "overdue", icon: "⚠" };
  if (days === 0) return { label: t("today"), cls: "today", icon: "📌" };
  if (days === 1) return { label: t("tomorrow"), cls: "", icon: "🗓" };
  return { label: t("inDays", { n: days }), cls: "", icon: "🗓" };
}

function repeatLabel(task: Task, t: ReturnType<typeof useI18n>["t"]) {
  if (task.recurrence === "daily") return { label: t("repeatDaily"), icon: "🔁" };
  if (task.recurrence === "weekly") return { label: t("repeatWeekly"), icon: "🔁" };
  if (task.recurrence === "monthly") return { label: t("repeatMonthly"), icon: "🔁" };
  return null;
}

export function TaskCard({ task, onToggleDone, onEdit, onDelete }: Props) {
  const { t } = useI18n();
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
    data: { task },
  });

  const due = dueInfo(task, t);
  const repeat = repeatLabel(task, t);

  return (
    <div
      ref={setNodeRef}
      className={`task-card ${task.done ? "completed" : ""} ${isDragging ? "dragging" : ""}`}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      {...attributes}
      {...listeners}
    >
      <div className="task-top">
        <input
          type="checkbox"
          className="task-check"
          checked={task.done === 1}
          onChange={() => onToggleDone(task)}
          onClick={(e) => e.stopPropagation()}
          onPointerDown={(e) => e.stopPropagation()}
        />
        <span className="task-title">{task.title}</span>
        <span className="task-actions">
          <button
            className="task-action"
            title={t("editTask")}
            onClick={(e) => { e.stopPropagation(); onEdit(task); }}
            onPointerDown={(e) => e.stopPropagation()}
          >
            ✏️
          </button>
          <button
            className="task-action del"
            title={t("delete")}
            onClick={(e) => { e.stopPropagation(); onDelete(task); }}
            onPointerDown={(e) => e.stopPropagation()}
          >
            🗑
          </button>
        </span>
      </div>
      {(due || repeat || task.notes) && (
        <div className="task-meta">
          {due && <span className={`meta-badge ${due.cls}`}>{due.icon} {due.label}</span>}
          {repeat && <span className="meta-badge repeat">{repeat.icon} {repeat.label}</span>}
          {task.notes && <span className="meta-badge">📝</span>}
        </div>
      )}
    </div>
  );
}
