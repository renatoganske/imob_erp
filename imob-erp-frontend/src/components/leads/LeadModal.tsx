"use client";

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { LEAD_SOURCE_LABEL, LEAD_STAGE_LABEL } from "@/lib/labels";
import type { Lead } from "@/types/lead";

export function LeadModal({ lead, onClose }: { lead: Lead | null; onClose: () => void }) {
  return (
    <Dialog open={!!lead} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        {lead && (
          <>
            <DialogHeader>
              <DialogTitle>{lead.name}</DialogTitle>
              <div>
                <Badge variant="info">{LEAD_STAGE_LABEL[lead.stage]}</Badge>
              </div>
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
                <span className="text-muted-foreground">Origem:</span> {LEAD_SOURCE_LABEL[lead.source]}
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
