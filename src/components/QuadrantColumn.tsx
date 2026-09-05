import { useDroppable } from "@dnd-kit/core";
import { SortableContext, verticalListSortingStrategy } from "@dnd-kit/sortable";
import type { Quadrant, Task } from "../types";
import { useI18n, type TKey } from "../i18n";
import { TaskCard } from "./TaskCard";

interface Props {
  quadrant: Quadrant;
  tasks: Task[];
  onToggleDone: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
}

const META: Record<Quadrant, { title: TKey; sub: TKey; cls: string }> = {
  do: { title: "importantUrgent", sub: "doSub", cls: "q-do" },
  schedule: { title: "importantNotUrgent", sub: "scheduleSub", cls: "q-schedule" },
  delegate: { title: "notImportantUrgent", sub: "delegateSub", cls: "q-delegate" },
  eliminate: { title: "notImportantNotUrgent", sub: "eliminateSub", cls: "q-eliminate" },
};

export function QuadrantColumn({ quadrant, tasks, onToggleDone, onEdit, onDelete }: Props) {
  const { t } = useI18n();
  const { setNodeRef, isOver } = useDroppable({ id: `q-${quadrant}`, data: { quadrant } });
  const meta = META[quadrant];

  return (
    <section ref={setNodeRef} className={`quadrant ${meta.cls} ${isOver ? "drag-over" : ""}`}>
      <div className="quadrant-head">
        <span className="quadrant-dot" />
        <h3>{t(meta.title)}</h3>
        <span className="q-count">{tasks.length}</span>
      </div>
      <div className="sub">{t(meta.sub)}</div>
      <SortableContext items={tasks.map((t) => t.id)} strategy={verticalListSortingStrategy}>
        <div className="task-list">
          {tasks.length === 0 && <div className="empty-hint">{t("noTasks")}</div>}
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} onToggleDone={onToggleDone} onEdit={onEdit} onDelete={onDelete} />
          ))}
        </div>
      </SortableContext>
    </section>
  );
}
