"use client";

import { useState } from "react";
import { LEAD_STAGE_LABEL } from "@/lib/labels";
import { LEAD_STAGES } from "@/types/lead";
import type { Lead, LeadStage } from "@/types/lead";
import { LeadCard } from "./LeadCard";
import { LeadModal } from "./LeadModal";

function scrollToStage(stage: LeadStage) {
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  document
    .getElementById(`stage-${stage}`)
    ?.scrollIntoView({ behavior: reduce ? "auto" : "smooth", inline: "center", block: "nearest" });
}

export function LeadKanban({
  leads,
  onMoveStage,
}: {
  leads: Lead[];
  onMoveStage: (id: string, stage: LeadStage) => void;
}) {
  const [selected, setSelected] = useState<Lead | null>(null);

  return (
    <div className="flex flex-col gap-4">
      {/* Resumo do funil: mostra todas as etapas de uma vez e leva até a coluna (as últimas ficam além da rolagem). */}
      <nav aria-label="Resumo por etapa" className="flex flex-wrap gap-2">
        {LEAD_STAGES.map((stage) => (
          <button
            key={stage}
            type="button"
            onClick={() => scrollToStage(stage)}
            className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs transition-colors hover:border-primary/40"
          >
            {LEAD_STAGE_LABEL[stage]}
            <span className="font-semibold tabular-nums">{leads.filter((lead) => lead.stage === stage).length}</span>
          </button>
        ))}
      </nav>

      <div className="flex snap-x gap-3 overflow-x-auto pb-4">
        {LEAD_STAGES.map((stage) => {
          const items = leads.filter((lead) => lead.stage === stage);
          return (
            <section
              key={stage}
              id={`stage-${stage}`}
              aria-label={LEAD_STAGE_LABEL[stage]}
              className="flex w-64 shrink-0 snap-start flex-col gap-3 rounded-lg bg-secondary/60 p-3"
            >
              <h3 className="flex items-center justify-between text-sm font-medium">
                {LEAD_STAGE_LABEL[stage]}
                <span className="rounded-full bg-background px-2 py-0.5 text-xs text-muted-foreground">{items.length}</span>
              </h3>
              <div className="flex flex-col gap-2">
                {items.map((lead) => (
                  <LeadCard
                    key={lead.id}
                    lead={lead}
                    onOpen={() => setSelected(lead)}
                    onMove={(newStage) => onMoveStage(lead.id, newStage)}
                  />
                ))}
                {items.length === 0 && <p className="py-4 text-center text-xs text-muted-foreground">Nenhum lead</p>}
              </div>
            </section>
          );
        })}
      </div>
      <LeadModal lead={selected} onClose={() => setSelected(null)} />
    </div>
  );
}
