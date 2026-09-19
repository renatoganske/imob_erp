import { formatCurrency } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import type { FinancialDashboard as Dashboard } from "@/types/financial";

export function FinancialDashboard({ data }: { data: Dashboard }) {
  const cards = [
    { label: "Saldo do mês", value: data.saldoDoMes },
    { label: "Total a receber", value: data.totalAReceber },
    { label: "Total a pagar", value: data.totalAPagar },
    { label: "Inadimplência", value: data.inadimplencia, danger: true },
  ];

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map((card) => (
        <Card key={card.label} className="p-4">
          <p className="text-sm text-muted-foreground">{card.label}</p>
          <p
            className={`mt-1 text-2xl font-semibold tabular-nums tracking-tight ${card.danger ? "text-danger" : ""}`}
          >
            {formatCurrency(card.value)}
          </p>
        </Card>
      ))}
    </div>
  );
}
