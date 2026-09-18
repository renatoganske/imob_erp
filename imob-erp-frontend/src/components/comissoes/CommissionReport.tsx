import { formatCurrency } from "@/lib/utils";
import type { CommissionReportItem } from "@/types/commission";

export function CommissionReport({ items }: { items: CommissionReportItem[] }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((item) => (
        <div key={item.agentId} className="rounded-lg border border-border p-4">
          <p className="text-sm text-muted-foreground">Corretor {item.agentId.slice(0, 8)}</p>
          <p className="mt-1 text-2xl font-bold">{formatCurrency(item.total)}</p>
        </div>
      ))}
    </div>
  );
}
