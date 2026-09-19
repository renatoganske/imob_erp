"use client";

import { Receipt } from "lucide-react";
import { EntryForm } from "@/components/financeiro/EntryForm";
import { EntryList } from "@/components/financeiro/EntryList";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { useFinancialEntries, useFinancialMutations } from "@/hooks/useFinancial";

export default function AccountsPayablePage() {
  const { data, loading, reload } = useFinancialEntries({ type: "DESPESA" });
  const { pay, cancel } = useFinancialMutations();

  async function handlePay(id: string) {
    await pay(id);
    reload();
  }

  async function handleCancel(id: string) {
    await cancel(id);
    reload();
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Contas a pagar" description="Despesas, repasses e comissões a pagar." />
      <EntryForm type="DESPESA" onCreated={reload} />
      {loading && <ListSkeleton />}
      {data && data.content.length === 0 && (
        <EmptyState icon={Receipt} title="Nenhuma conta a pagar" description="Lance a primeira despesa no formulário acima." />
      )}
      {data && data.content.length > 0 && <EntryList entries={data.content} onPay={handlePay} onCancel={handleCancel} />}
    </div>
  );
}
