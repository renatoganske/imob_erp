"use client";

import { useState } from "react";
import type { Visit } from "@/types/visit";
import { VisitCard } from "./VisitCard";
import { VisitModal } from "./VisitModal";

export function VisitList({
  visits,
  onSaveResult,
}: {
  visits: Visit[];
  onSaveResult: (id: string, result: string) => void;
}) {
  const [selected, setSelected] = useState<Visit | null>(null);

  return (
    <div className="flex flex-col gap-2">
      {visits.map((visit) => (
        <VisitCard key={visit.id} visit={visit} onOpen={() => setSelected(visit)} />
      ))}
      <VisitModal
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
