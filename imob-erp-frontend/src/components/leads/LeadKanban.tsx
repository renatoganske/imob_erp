"use client";

import { useState } from "react";
import { LEAD_STAGES } from "@/types/lead";
import type { Lead, LeadStage } from "@/types/lead";
import { LeadCard } from "./LeadCard";
import { LeadModal } from "./LeadModal";

const STAGE_LABEL: Record<LeadStage, string> = {
  NOVO: "Novo",
  EM_ATENDIMENTO: "Em atendimento",
  VISITA_AGENDADA: "Visita agendada",
  PROPOSTA: "Proposta",
  FECHADO: "Fechado",
  PERDIDO: "Perdido",
};

export function LeadKanban({
  leads,
  onMoveStage,
}: {
  leads: Lead[];
  onMoveStage: (id: string, stage: LeadStage) => void;
}) {
  const [selected, setSelected] = useState<Lead | null>(null);

  return (
    <div className="flex gap-4 overflow-x-auto pb-4">
      {LEAD_STAGES.map((stage) => (
        <div key={stage} className="flex w-64 shrink-0 flex-col gap-3">
          <h3 className="text-sm font-semibold text-muted-foreground">{STAGE_LABEL[stage]}</h3>
          <div className="flex flex-col gap-2">
            {leads
              .filter((lead) => lead.stage === stage)
              .map((lead) => (
                <LeadCard
                  key={lead.id}
                  lead={lead}
                  onOpen={() => setSelected(lead)}
                  onMove={(newStage) => onMoveStage(lead.id, newStage)}
                />
              ))}
          </div>
        </div>
      ))}
      <LeadModal lead={selected} onClose={() => setSelected(null)} />
    </div>
  );
}
