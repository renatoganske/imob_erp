"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { Visit } from "@/types/visit";

export function VisitModal({
  visit,
  onClose,
  onSaveResult,
}: {
  visit: Visit | null;
  onClose: () => void;
  onSaveResult: (result: string) => void;
}) {
  const [result, setResult] = useState(visit?.result ?? "");

  return (
    <Dialog open={!!visit} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        {visit && (
          <>
            <DialogHeader>
              <DialogTitle>Resultado da visita</DialogTitle>
            </DialogHeader>
            <textarea
              className="min-h-24 w-full rounded-md border border-border bg-card px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
              value={result}
              onChange={(e) => setResult(e.target.value)}
              placeholder="Descreva o resultado da visita..."
            />
            <Button className="mt-4" onClick={() => onSaveResult(result)}>
              Salvar
            </Button>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
