"use client";

import { ArrowRight, Phone } from "lucide-react";
import { Select } from "@/components/ui/select";
import { LEAD_STAGE_LABEL } from "@/lib/labels";
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
  // "Perdido" é terminal e fica fora da sequência do funil.
  const nextStage = lead.stage === "PERDIDO" ? undefined : LEAD_STAGES[currentIndex + 1];
  const otherStages = LEAD_STAGES.filter((stage) => stage !== lead.stage && stage !== nextStage);

  return (
    <div className="flex flex-col gap-2 rounded-md border border-border bg-card p-3 text-sm shadow-card">
      <button onClick={onOpen} className="text-left font-medium hover:text-primary">
        {lead.name}
      </button>
      <p className="flex items-center gap-1.5 text-muted-foreground">
        <Phone className="h-3.5 w-3.5" />
        {lead.phone}
      </p>
      <div className="mt-1 flex flex-col gap-2">
        {nextStage && (
          <button
            onClick={() => onMove(nextStage)}
            className="flex items-center justify-between rounded-md bg-primary-soft px-2.5 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary-soft/70"
          >
            Mover para {LEAD_STAGE_LABEL[nextStage]}
            <ArrowRight className="h-3 w-3 shrink-0" />
          </button>
        )}
        <Select
          aria-label={`Mover ${lead.name} para outra etapa`}
          className="h-8 w-full px-2 text-xs text-muted-foreground"
          value=""
          onChange={(e) => e.target.value && onMove(e.target.value as LeadStage)}
        >
          <option value="">Outra etapa…</option>
          {otherStages.map((stage) => (
            <option key={stage} value={stage}>
              {LEAD_STAGE_LABEL[stage]}
            </option>
          ))}
        </Select>
      </div>
    </div>
  );
}
