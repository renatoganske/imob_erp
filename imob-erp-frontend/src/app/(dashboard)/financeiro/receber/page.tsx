"use client";

import { EntryForm } from "@/components/financeiro/EntryForm";
import { EntryList } from "@/components/financeiro/EntryList";
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
      <h1 className="text-2xl font-bold">Contas a receber</h1>
      <EntryForm onCreated={reload} />
      {loading && <p className="text-muted-foreground">Carregando...</p>}
      {data && <EntryList entries={data.content} onPay={handlePay} onCancel={handleCancel} />}
    </div>
  );
}
