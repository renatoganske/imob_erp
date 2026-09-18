"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { LeadFormModal } from "@/components/leads/LeadFormModal";
import { LeadKanban } from "@/components/leads/LeadKanban";
import { useLeadMutations, useLeads } from "@/hooks/useLeads";

export default function LeadsPage() {
  const { data, loading, reload } = useLeads();
  const { updateStage } = useLeadMutations();
  const [creating, setCreating] = useState(false);

  async function handleMoveStage(id: string, stage: Parameters<typeof updateStage>[1]) {
    await updateStage(id, stage);
    reload();
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Leads</h1>
        <Button onClick={() => setCreating(true)}>Novo lead</Button>
      </div>
      {loading && <p className="text-muted-foreground">Carregando...</p>}
      {data && <LeadKanban leads={data.content} onMoveStage={handleMoveStage} />}
      <LeadFormModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={() => {
          setCreating(false);
          reload();
        }}
      />
    </div>
  );
}
