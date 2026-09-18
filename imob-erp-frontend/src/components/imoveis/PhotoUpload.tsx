"use client";

import { useAuth } from "@clerk/nextjs";
import { useState } from "react";
import { Button } from "@/components/ui/button";

const MAX_PHOTOS = 20;

export function PhotoUpload({
  propertyId,
  photos,
  onUploaded,
}: {
  propertyId: string;
  photos: string[];
  onUploaded: () => void;
}) {
  const { getToken } = useAuth();
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setError(null);
    try {
      const token = await getToken();
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/properties/${propertyId}/photos`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        body: formData,
      });

      if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.error ?? "Falha ao enviar foto");
      }

      onUploaded();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao enviar foto");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
        {photos.map((url) => (
          <img key={url} src={url} alt="Foto do imóvel" className="aspect-square rounded-md object-cover" />
        ))}
      </div>
      {photos.length < MAX_PHOTOS && (
        <label>
          <Button asChild variant="outline" disabled={uploading}>
            <span>{uploading ? "Enviando..." : "Adicionar foto"}</span>
          </Button>
          <input type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={handleFileChange} />
        </label>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
