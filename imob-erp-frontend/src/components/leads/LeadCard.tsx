"use client";

import { MoreHorizontal } from "lucide-react";
import { LEAD_STAGES } from "@/types/lead";
import type { Lead, LeadStage } from "@/types/lead";

export function LeadCard({
  lead,
  onOpen,
  onMove,
}: {
  lead: Lead;
  onOpen: () => void;
  onMove: (stage: LeadStage) => void;
}) {
  const currentIndex = LEAD_STAGES.indexOf(lead.stage);
  const nextStage = LEAD_STAGES[currentIndex + 1];

  return (
    <div className="flex flex-col gap-2 rounded-md border border-border bg-background p-3 text-sm">
      <div className="flex items-start justify-between">
        <button onClick={onOpen} className="text-left font-medium hover:underline">
          {lead.name}
        </button>
        <MoreHorizontal className="h-4 w-4 text-muted-foreground" />
      </div>
      <p className="text-muted-foreground">{lead.phone}</p>
      {nextStage && (
        <button
          onClick={() => onMove(nextStage)}
          className="self-start text-xs font-medium text-primary hover:underline"
        >
          Mover para {nextStage.replace("_", " ")} →
        </button>
      )}
    </div>
  );
}
