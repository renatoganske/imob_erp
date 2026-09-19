"use client";

import { useAuth } from "@clerk/nextjs";
import { FileText } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";

export function ContractDocumentUpload({
  contractId,
  documentUrl,
  onUploaded,
}: {
  contractId: string;
  documentUrl?: string;
  onUploaded: () => void;
}) {
  const { getToken } = useAuth();
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const token = await getToken();
      const formData = new FormData();
      formData.append("file", file);
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/contracts/${contractId}/document`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        body: formData,
      });
      if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.error ?? "Falha ao enviar o documento");
      }
      setFile(null);
      onUploaded();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao enviar o documento");
    } finally {
      setUploading(false);
    }
  }

  return (
    <section className="flex flex-col gap-3" aria-label="Documento do contrato">
      <h2 className="text-sm font-medium">Documento (PDF)</h2>
      {documentUrl && (
        <a href={documentUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 text-sm text-primary hover:underline">
          <FileText className="h-4 w-4" />
          Ver documento atual
        </a>
      )}
      <div className="flex flex-wrap items-center gap-3">
        <label>
          <Button asChild variant="outline" disabled={uploading}>
            <span>{documentUrl ? "Substituir PDF" : "Selecionar PDF"}</span>
          </Button>
          <input
            type="file"
            accept="application/pdf"
            aria-label="Arquivo PDF do contrato"
            className="hidden"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </label>
        {file && (
          <>
            <span className="max-w-full truncate text-sm" data-testid="selected-file">
              {file.name}
            </span>
            <Button onClick={handleUpload} disabled={uploading}>
              {uploading ? "Enviando..." : "Enviar"}
            </Button>
          </>
        )}
      </div>
      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}
    </section>
  );
}
