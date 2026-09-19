"use client";

import { useState } from "react";
import type { Visit } from "@/types/visit";
import { VisitCard } from "./VisitCard";
import { VisitModal } from "./VisitModal";

export function VisitList({
  visits,
  propertyTitles,
  leadNames,
  onSaveResult,
}: {
  visits: Visit[];
  propertyTitles: Record<string, string>;
  leadNames: Record<string, string>;
  onSaveResult: (id: string, result: string) => void;
}) {
  const [selected, setSelected] = useState<Visit | null>(null);

  return (
    <div className="flex flex-col gap-2">
      {visits.map((visit) => (
        <VisitCard
          key={visit.id}
          visit={visit}
          propertyTitle={propertyTitles[visit.propertyId]}
          leadName={leadNames[visit.leadId]}
          onOpen={() => setSelected(visit)}
        />
      ))}
      <VisitModal
        key={selected?.id ?? "none"}
        visit={selected}
        onClose={() => setSelected(null)}
        onSaveResult={(result) => {
          if (selected) onSaveResult(selected.id, result);
          setSelected(null);
        }}
      />
    </div>
  );
}
