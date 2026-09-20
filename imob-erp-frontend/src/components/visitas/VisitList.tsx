"use client";

import { useState } from "react";
import { formatGroupDate, groupVisitsByDate } from "@/lib/visits";
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
    <div className="flex flex-col gap-6">
      {groupVisitsByDate(visits).map(({ date, visits: dayVisits }) => (
        <section key={date} aria-labelledby={`visits-${date}`} className="flex flex-col gap-2">
          <h2 id={`visits-${date}`} className="text-sm font-semibold capitalize text-muted-foreground">
            {formatGroupDate(date)}
          </h2>
          {dayVisits.map((visit) => (
            <VisitCard
              key={visit.id}
              visit={visit}
              propertyTitle={propertyTitles[visit.propertyId]}
              leadName={leadNames[visit.leadId]}
              onOpen={() => setSelected(visit)}
            />
          ))}
        </section>
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
