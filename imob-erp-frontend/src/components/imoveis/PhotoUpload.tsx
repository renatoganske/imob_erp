"use client";

import { useAuth } from "@clerk/nextjs";
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type Announcements,
  type DragEndEvent,
} from "@dnd-kit/core";
import { SortableContext, rectSortingStrategy, sortableKeyboardCoordinates, useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GripVertical, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";

export const MAX_PHOTOS = 20;
export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
export const UPLOAD_CONCURRENCY = 3;
const ACCEPTED_TYPES = ["image/png", "image/jpeg", "image/webp"];

type Status = "uploading" | "done" | "error";

interface Entry {
  id: number;
  file: File;
}

interface UploadItem {
  id: number;
  name: string;
  status: Status;
  error?: string;
}

interface Plan {
  accepted: Entry[];
  rejected: (Entry & { reason: string })[];
  overLimit: number;
}

const invalidReason = (file: File): string | null =>
  !ACCEPTED_TYPES.includes(file.type)
    ? "Formato não permitido (use JPG, PNG ou WebP)"
    : file.size > MAX_PHOTO_BYTES
      ? "Acima do limite de 10 MB"
      : null;

/** Decide, sem efeitos colaterais, o que enviar e o que recusar (formato, tamanho e limite de fotos do imóvel). */
export function planUpload(files: File[], alreadyStored: number): Plan {
  const room = Math.max(MAX_PHOTOS - alreadyStored, 0);
  const checked = files.map((file, id) => ({ id, file, reason: invalidReason(file) }));
  const valid = checked.filter(({ reason }) => reason === null);
  const invalid = checked.filter(({ reason }) => reason !== null);

  return {
    accepted: valid.slice(0, room).map(({ id, file }) => ({ id, file })),
    rejected: [
      ...invalid.map(({ id, file, reason }) => ({ id, file, reason: reason as string })),
      ...valid.slice(room).map(({ id, file }) => ({ id, file, reason: `Limite de ${MAX_PHOTOS} fotos atingido` })),
    ],
    overLimit: Math.max(valid.length - room, 0),
  };
}

/** Chave aceita pelo DELETE: último segmento do caminho da URL da foto, sem query string. */
export const photoKey = (url: string): string => url.split("?")[0].split("/").filter(Boolean).at(-1) ?? "";

/** Nova lista com o item de `from` movido para `to`; índices fora da lista devolvem uma cópia igual. */
export function moveItem<T>(items: readonly T[], from: number, to: number): T[] {
  const inRange = (i: number) => i >= 0 && i < items.length;
  if (!inRange(from) || !inRange(to) || from === to) return [...items];
  const rest = items.filter((_, i) => i !== from);
  return [...rest.slice(0, to), items[from], ...rest.slice(to)];
}

const position = (order: readonly string[], id: unknown): number => order.indexOf(String(id)) + 1;

const announcementsFor = (order: readonly string[]): Announcements => ({
  onDragStart: ({ active }) => `Foto ${position(order, active.id)} pega. Use as setas para mover.`,
  onDragOver: ({ active, over }) =>
    over ? `Foto ${position(order, active.id)} está sobre a posição ${position(order, over.id)}.` : undefined,
  onDragEnd: ({ active, over }) =>
    over
      ? `Foto ${position(order, active.id)} movida para a posição ${position(order, over.id)}.`
      : `Foto ${position(order, active.id)} solta sem mudar de posição.`,
  onDragCancel: ({ active }) => `Movimento cancelado. Foto ${position(order, active.id)} voltou ao lugar.`,
});

const chunk = <T,>(items: T[], size: number): T[][] =>
  Array.from({ length: Math.ceil(items.length / size) }, (_, i) => items.slice(i * size, (i + 1) * size));

/** Envia em lotes de `size` em paralelo, um lote após o outro; devolve o resultado de cada envio, na ordem. */
const sendInBatches = <T,>(items: T[], size: number, send: (item: T) => Promise<boolean>): Promise<boolean[]> =>
  chunk(items, size).reduce<Promise<boolean[]>>(
    async (done, batch) => [...(await done), ...(await Promise.all(batch.map(send)))],
    Promise.resolve([]),
  );

const overLimitNotice = (count: number): string | null =>
  count > 0 ? `${count} foto(s) ignorada(s): o imóvel aceita no máximo ${MAX_PHOTOS} fotos.` : null;

const withStatus = (items: UploadItem[], id: number, patch: Partial<UploadItem>): UploadItem[] =>
  items.map((item) => (item.id === id ? { ...item, ...patch } : item));

interface TileProps {
  url: string;
  index: number;
  canReorder: boolean;
  canDelete: boolean;
  disabled: boolean;
  onDelete: (url: string) => void;
}

function PhotoTile({ url, index, canReorder, canDelete, disabled, onDelete }: TileProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: url,
    disabled: !canReorder || disabled,
  });

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`relative ${isDragging ? "z-10 opacity-80" : ""}`}
    >
      <img src={url} alt="Foto do imóvel" draggable={false} className="aspect-square w-full rounded-md object-cover" />
      {index === 0 && (
        <span className="absolute left-1 top-1 rounded bg-background/90 px-1.5 py-0.5 text-xs font-medium">Capa</span>
      )}
      {canReorder && (
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label={`Reordenar foto ${index + 1}`}
          disabled={disabled}
          className="absolute bottom-1 left-1 h-8 w-8 cursor-grab touch-none bg-background/90"
          {...attributes}
          {...listeners}
        >
          <GripVertical className="h-4 w-4" aria-hidden />
        </Button>
      )}
      {canDelete && (
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label={`Excluir foto ${index + 1}`}
          className="absolute right-1 top-1 h-8 w-8 bg-background/90"
          onClick={() => onDelete(url)}
        >
          <Trash2 className="h-4 w-4" aria-hidden />
        </Button>
      )}
    </div>
  );
}

export function PhotoUpload({
  propertyId,
  photos,
  onUploaded,
  canDelete = false,
  canReorder = false,
}: {
  propertyId: string;
  photos: string[];
  onUploaded: () => void;
  canDelete?: boolean;
  canReorder?: boolean;
}) {
  const { getToken } = useAuth();
  const [busy, setBusy] = useState(false);
  const [items, setItems] = useState<UploadItem[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [order, setOrder] = useState<string[]>(photos);
  const [reordering, setReordering] = useState(false);
  const [orderError, setOrderError] = useState<string | null>(null);

  // A galeria do servidor é a fonte da verdade; a ordem local só antecipa o resultado do arraste.
  useEffect(() => setOrder(photos), [photos]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const setStatus = (id: number, patch: Partial<UploadItem>) => setItems((current) => withStatus(current, id, patch));

  async function upload({ id, file }: Entry): Promise<boolean> {
    try {
      const token = await getToken();
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/properties/${propertyId}/photos`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        body,
      });
      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        throw new Error(problem?.error ?? "Falha ao enviar foto");
      }
      setStatus(id, { status: "done" });
      return true;
    } catch (err) {
      setStatus(id, { status: "error", error: err instanceof Error ? err.message : "Falha ao enviar foto" });
      return false;
    }
  }

  async function confirmDelete(url: string) {
    setDeleting(true);
    setDeleteError(null);
    try {
      const token = await getToken();
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/v1/properties/${propertyId}/photos/${encodeURIComponent(photoKey(url))}`,
        { method: "DELETE", headers: token ? { Authorization: `Bearer ${token}` } : undefined },
      );
      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        throw new Error(problem?.error ?? "Falha ao excluir foto");
      }
      setPendingDelete(null);
      onUploaded();
    } catch (err) {
      setPendingDelete(null);
      setDeleteError(err instanceof Error ? err.message : "Falha ao excluir foto");
    } finally {
      setDeleting(false);
    }
  }

  async function saveOrder(next: string[], previous: string[]) {
    setOrder(next);
    setOrderError(null);
    setReordering(true);
    try {
      const token = await getToken();
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/properties/${propertyId}/photos/order`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify({ keys: next.map(photoKey) }),
      });
      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        throw new Error(problem?.error ?? "Falha ao reordenar fotos");
      }
      onUploaded();
    } catch (err) {
      setOrder(previous);
      setOrderError(err instanceof Error ? err.message : "Falha ao reordenar fotos");
    } finally {
      setReordering(false);
    }
  }

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (!over || active.id === over.id) return;
    const from = order.indexOf(String(active.id));
    const to = order.indexOf(String(over.id));
    if (from < 0 || to < 0) return;
    void saveOrder(moveItem(order, from, to), order);
  }

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(e.target.files ?? []);
    e.target.value = "";
    if (selected.length === 0) return;

    const { accepted, rejected, overLimit } = planUpload(selected, photos.length);
    setNotice(overLimitNotice(overLimit));
    setItems([
      ...accepted.map(({ id, file }): UploadItem => ({ id, name: file.name, status: "uploading" })),
      ...rejected.map(({ id, file, reason }): UploadItem => ({ id, name: file.name, status: "error", error: reason })),
    ]);
    if (accepted.length === 0) return;

    setBusy(true);
    const results = await sendInBatches(accepted, UPLOAD_CONCURRENCY, upload);
    setBusy(false);
    // Recarrega a galeria uma única vez, ao final do lote.
    if (results.some(Boolean)) onUploaded();
  }

  return (
    <div className="flex flex-col gap-3">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
        accessibility={{
          announcements: announcementsFor(order),
          screenReaderInstructions: {
            draggable:
              "Para reordenar, pressione espaço, mova com as setas e pressione espaço novamente para soltar. Esc cancela.",
          },
        }}
      >
        <SortableContext items={order} strategy={rectSortingStrategy}>
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
            {order.map((url, index) => (
              <PhotoTile
                key={url}
                url={url}
                index={index}
                canReorder={canReorder && order.length > 1}
                canDelete={canDelete}
                disabled={reordering}
                onDelete={setPendingDelete}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>

      {orderError && (
        <p role="alert" className="text-sm text-danger">
          {orderError}
        </p>
      )}

      {deleteError && (
        <p role="alert" className="text-sm text-danger">
          {deleteError}
        </p>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Excluir foto"
        description="A foto será removida do imóvel e apagada definitivamente. Esta ação não pode ser desfeita."
        confirmLabel="Excluir"
        loading={deleting}
        onConfirm={() => pendingDelete && confirmDelete(pendingDelete)}
        onCancel={() => setPendingDelete(null)}
      />

      {photos.length < MAX_PHOTOS && (
        <label>
          <Button asChild variant="outline" disabled={busy}>
            <span>{busy ? "Enviando..." : "Adicionar fotos"}</span>
          </Button>
          <input
            type="file"
            multiple
            accept="image/png,image/jpeg,image/webp"
            className="hidden"
            disabled={busy}
            onChange={handleFileChange}
          />
        </label>
      )}
      <p className="text-xs text-muted-foreground">
        {photos.length}/{MAX_PHOTOS} fotos · JPG, PNG ou WebP até 10 MB cada
      </p>

      {notice && <p className="text-sm text-warning">{notice}</p>}

      {items.length > 0 && (
        <ul aria-live="polite" className="flex flex-col gap-1 text-sm">
          {items.map((item) => (
            <li key={item.id} className="flex items-baseline justify-between gap-3">
              <span className="truncate">{item.name}</span>
              {item.status === "uploading" && <span className="text-muted-foreground">Enviando…</span>}
              {item.status === "done" && <span className="text-success">Enviada</span>}
              {item.status === "error" && (
                <span role="alert" className="text-danger">
                  {item.error}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
