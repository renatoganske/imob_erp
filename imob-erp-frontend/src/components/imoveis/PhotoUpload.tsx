"use client";

import { useAuth } from "@clerk/nextjs";
import { Trash2 } from "lucide-react";
import { useState } from "react";
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

export function PhotoUpload({
  propertyId,
  photos,
  onUploaded,
  canDelete = false,
}: {
  propertyId: string;
  photos: string[];
  onUploaded: () => void;
  canDelete?: boolean;
}) {
  const { getToken } = useAuth();
  const [busy, setBusy] = useState(false);
  const [items, setItems] = useState<UploadItem[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

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
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
        {photos.map((url, index) => (
          <div key={url} className="relative">
            <img src={url} alt="Foto do imóvel" className="aspect-square w-full rounded-md object-cover" />
            {canDelete && (
              <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label={`Excluir foto ${index + 1}`}
                className="absolute right-1 top-1 h-8 w-8 bg-background/90"
                onClick={() => setPendingDelete(url)}
              >
                <Trash2 className="h-4 w-4" aria-hidden />
              </Button>
            )}
          </div>
        ))}
      </div>

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
