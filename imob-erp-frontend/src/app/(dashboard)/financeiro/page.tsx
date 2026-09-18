"use client";

import { FinancialDashboard } from "@/components/financeiro/FinancialDashboard";
import { useFinancialDashboard } from "@/hooks/useFinancial";

export default function FinancialPage() {
  const { data, loading } = useFinancialDashboard();

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Financeiro</h1>
      {loading && <p className="text-muted-foreground">Carregando...</p>}
      {data && <FinancialDashboard data={data} />}
    </div>
  );
}
