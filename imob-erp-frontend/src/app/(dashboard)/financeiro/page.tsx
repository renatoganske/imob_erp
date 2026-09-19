"use client";

import { FinancialDashboard } from "@/components/financeiro/FinancialDashboard";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/state";
import { useFinancialDashboard } from "@/hooks/useFinancial";

export default function FinancialPage() {
  const { data, loading } = useFinancialDashboard();

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Financeiro" description="Saldo, contas a receber e a pagar." />
      {loading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      )}
      {data && <FinancialDashboard data={data} />}
    </div>
  );
}
