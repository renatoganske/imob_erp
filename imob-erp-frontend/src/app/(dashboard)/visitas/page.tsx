"use client";

import { useMemo, useState } from "react";
import { CalendarCheck, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { VisitFormModal } from "@/components/visitas/VisitFormModal";
import { VisitList } from "@/components/visitas/VisitList";
import { useLeads } from "@/hooks/useLeads";
import { useProperties } from "@/hooks/useProperties";
import { useVisitMutations, useVisits } from "@/hooks/useVisits";

export default function VisitsPage() {
  const { data, loading, reload } = useVisits();
  const { data: properties } = useProperties();
  const { data: leads } = useLeads();
  const { updateResult } = useVisitMutations();
  const [scheduling, setScheduling] = useState(false);

  const propertyTitles = useMemo(
    () => Object.fromEntries((properties?.content ?? []).map((p) => [p.id, p.title])),
    [properties]
  );
  const leadNames = useMemo(() => Object.fromEntries((leads?.content ?? []).map((l) => [l.id, l.name])), [leads]);

  async function handleSaveResult(id: string, result: string) {
    await updateResult(id, result);
    reload();
  }

  const scheduleButton = (
    <Button onClick={() => setScheduling(true)}>
      <Plus className="h-4 w-4" />
      Agendar visita
    </Button>
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Visitas" description="Agenda de visitas e resultado de cada uma." actions={scheduleButton} />
      {loading && <ListSkeleton />}
      {data && data.content.length === 0 && (
        <EmptyState
          icon={CalendarCheck}
          title="Nenhuma visita agendada"
          description="Agende a primeira visita escolhendo o lead, o imóvel e o horário."
          action={scheduleButton}
        />
      )}
      {data && data.content.length > 0 && (
        <VisitList
          visits={data.content}
          propertyTitles={propertyTitles}
          leadNames={leadNames}
          onSaveResult={handleSaveResult}
        />
      )}
      <VisitFormModal
        open={scheduling}
        onClose={() => setScheduling(false)}
        onCreated={() => {
          setScheduling(false);
          reload();
        }}
      />
    </div>
  );
}
