"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { ListSkeleton } from "@/components/ui/state";
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
      <PageHeader
        title="Leads"
        description="Acompanhe cada contato do primeiro atendimento ao fechamento."
        actions={
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4" />
            Novo lead
          </Button>
        }
      />
      {loading && <ListSkeleton rows={4} />}
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
