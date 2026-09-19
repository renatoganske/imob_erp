import { Card } from "@/components/ui/card";
import { formatCurrency } from "@/lib/utils";
import type { CommissionReportItem } from "@/types/commission";

export function CommissionReport({
  items,
  agentNames,
}: {
  items: CommissionReportItem[];
  agentNames: Record<string, string>;
}) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((item) => (
        <Card key={item.agentId} className="p-4">
          <p className="text-sm text-muted-foreground">{agentNames[item.agentId] ?? `Corretor ${item.agentId.slice(0, 8)}`}</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums tracking-tight">{formatCurrency(item.total)}</p>
        </Card>
      ))}
    </div>
  );
}
