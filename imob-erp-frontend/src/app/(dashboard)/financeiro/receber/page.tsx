"use client";

import { Receipt } from "lucide-react";
import { EntryForm } from "@/components/financeiro/EntryForm";
import { EntryList } from "@/components/financeiro/EntryList";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { useFinancialEntries, useFinancialMutations } from "@/hooks/useFinancial";

export default function AccountsReceivablePage() {
  const { data, loading, reload } = useFinancialEntries({ type: "RECEITA" });
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
      <PageHeader title="Contas a receber" description="Aluguéis, parcelas de venda e outras receitas." />
      <EntryForm type="RECEITA" onCreated={reload} />
      {loading && <ListSkeleton />}
      {data && data.content.length === 0 && (
        <EmptyState icon={Receipt} title="Nenhuma conta a receber" description="Lance a primeira receita no formulário acima." />
      )}
      {data && data.content.length > 0 && <EntryList entries={data.content} onPay={handlePay} onCancel={handleCancel} />}
    </div>
  );
}
