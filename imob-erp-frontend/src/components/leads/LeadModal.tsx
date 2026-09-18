"use client";

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { Lead } from "@/types/lead";

export function LeadModal({ lead, onClose }: { lead: Lead | null; onClose: () => void }) {
  return (
    <Dialog open={!!lead} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        {lead && (
          <>
            <DialogHeader>
              <DialogTitle>{lead.name}</DialogTitle>
            </DialogHeader>
            <div className="flex flex-col gap-2 text-sm">
              <p>
                <span className="text-muted-foreground">Telefone:</span> {lead.phone}
              </p>
              {lead.email && (
                <p>
                  <span className="text-muted-foreground">E-mail:</span> {lead.email}
                </p>
              )}
              <p>
                <span className="text-muted-foreground">Origem:</span> {lead.source}
              </p>
              {lead.notes && (
                <p>
                  <span className="text-muted-foreground">Notas:</span> {lead.notes}
                </p>
              )}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
