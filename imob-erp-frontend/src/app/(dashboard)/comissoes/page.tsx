"use client";

import { useMemo } from "react";
import { Percent } from "lucide-react";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { CommissionList } from "@/components/comissoes/CommissionList";
import { CommissionReport } from "@/components/comissoes/CommissionReport";
import { useUsers } from "@/hooks/useUsers";
import { useCommissionMutations, useCommissionReport, useCommissions } from "@/hooks/useCommissions";

export default function CommissionsPage() {
  const { data, loading, reload } = useCommissions();
  const { data: report } = useCommissionReport();
  const { pay } = useCommissionMutations();
  const { data: users } = useUsers();
  const agentNames = useMemo(() => Object.fromEntries(users.map((u) => [u.id, u.name])), [users]);

  async function handlePay(id: string) {
    await pay(id);
    reload();
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title="Comissões" description="Total por corretor e comissões pendentes de pagamento." />

      <CommissionReport items={report} agentNames={agentNames} />

      {loading && <ListSkeleton />}
      {data && data.content.length === 0 && (
        <EmptyState
          icon={Percent}
          title="Nenhuma comissão ainda"
          description="As comissões são geradas quando um contrato é ativado, a partir da taxa do corretor."
        />
      )}
      {data && data.content.length > 0 && (
        <CommissionList commissions={data.content} agentNames={agentNames} onPay={handlePay} />
      )}
    </div>
  );
}
