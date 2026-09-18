"use client";

import { VisitList } from "@/components/visitas/VisitList";
import { useVisitMutations, useVisits } from "@/hooks/useVisits";

export default function VisitsPage() {
  const { data, loading, reload } = useVisits();
  const { updateResult } = useVisitMutations();

  async function handleSaveResult(id: string, result: string) {
    await updateResult(id, result);
    reload();
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Visitas</h1>
      {loading && <p className="text-muted-foreground">Carregando...</p>}
      {data && <VisitList visits={data.content} onSaveResult={handleSaveResult} />}
    </div>
  );
}
