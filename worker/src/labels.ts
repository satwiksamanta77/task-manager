import { all, first } from './db';

export interface LabelRow {
  id: string;
  user_id: string;
  name: string;
  color: string | null;
  created_at: string;
}

export interface Label {
  id: string;
  name: string;
  color: string | null;
  created_at: string;
}

export function toLabel(r: LabelRow): Label {
  return { id: r.id, name: r.name, color: r.color, created_at: r.created_at };
}

/** Labels attached to a task, ordered by name. */
export async function getLabelsForTask(db: D1Database, taskId: string): Promise<Label[]> {
  const rows = await all<LabelRow>(
    db,
    `SELECT l.id, l.user_id, l.name, l.color, l.created_at FROM labels l
     JOIN task_labels tl ON tl.label_id = l.id WHERE tl.task_id = ? ORDER BY l.name ASC`,
    taskId
  );
  return rows.map(toLabel);
}

/** Labels attached to a note, ordered by name. */
export async function getLabelsForNote(db: D1Database, noteId: string): Promise<Label[]> {
  const rows = await all<LabelRow>(
    db,
    `SELECT l.id, l.user_id, l.name, l.color, l.created_at FROM labels l
     JOIN note_labels nl ON nl.label_id = l.id WHERE nl.note_id = ? ORDER BY l.name ASC`,
    noteId
  );
  return rows.map(toLabel);
}

/**
 * Validate that every id in `labelIds` belongs to the user.
 * Returns the de-duplicated ids, or null when any id is unknown / not owned.
 */
export async function validateUserLabels(
  db: D1Database,
  userId: string,
  labelIds: string[]
): Promise<string[] | null> {
  const unique = [...new Set(labelIds)];
  for (const id of unique) {
    const row = await first<{ id: string }>(
      db,
      'SELECT id FROM labels WHERE id = ? AND user_id = ?',
      id,
      userId
    );
    if (!row) return null;
  }
  return unique;
}

/** Replace the label associations of a task. Assumes ids were validated. */
export async function setTaskLabels(db: D1Database, taskId: string, labelIds: string[]): Promise<void> {
  await db.prepare('DELETE FROM task_labels WHERE task_id = ?').bind(taskId).run();
  for (const id of labelIds) {
    await db.prepare('INSERT OR IGNORE INTO task_labels (task_id, label_id) VALUES (?, ?)').bind(taskId, id).run();
  }
}

/** Replace the label associations of a note. Assumes ids were validated. */
export async function setNoteLabels(db: D1Database, noteId: string, labelIds: string[]): Promise<void> {
  await db.prepare('DELETE FROM note_labels WHERE note_id = ?').bind(noteId).run();
  for (const id of labelIds) {
    await db.prepare('INSERT OR IGNORE INTO note_labels (note_id, label_id) VALUES (?, ?)').bind(noteId, id).run();
  }
}

/** Parse a `label_ids` request field: array of strings, or null when malformed. */
export function parseLabelIds(value: unknown): string[] | null {
  if (!Array.isArray(value) || !value.every((v) => typeof v === 'string')) return null;
  return [...new Set(value as string[])];
}
