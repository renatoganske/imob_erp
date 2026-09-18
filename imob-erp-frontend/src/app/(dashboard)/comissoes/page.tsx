"use client";

import { CommissionList } from "@/components/comissoes/CommissionList";
import { CommissionReport } from "@/components/comissoes/CommissionReport";
import { useCommissionMutations, useCommissionReport, useCommissions } from "@/hooks/useCommissions";

export default function CommissionsPage() {
  const { data, loading, reload } = useCommissions();
  const { data: report } = useCommissionReport();
  const { pay } = useCommissionMutations();

  async function handlePay(id: string) {
    await pay(id);
    reload();
  }

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="text-2xl font-bold">Comissões</h1>
      </div>

      <CommissionReport items={report} />

      {loading && <p className="text-muted-foreground">Carregando...</p>}
      {data && <CommissionList commissions={data.content} onPay={handlePay} />}
    </div>
  );
}
