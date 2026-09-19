import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { COMMISSION_STATUS_LABEL } from "@/lib/labels";
import { formatCurrency } from "@/lib/utils";
import type { Commission } from "@/types/commission";

export function CommissionList({
  commissions,
  agentNames,
  onPay,
}: {
  commissions: Commission[];
  agentNames: Record<string, string>;
  onPay: (id: string) => void;
}) {
  const agentName = (commission: Commission) => agentNames[commission.agentId] ?? commission.agentId.slice(0, 8);
  const statusBadge = (commission: Commission) => (
    <Badge variant={commission.status === "PAGO" ? "success" : "secondary"}>{COMMISSION_STATUS_LABEL[commission.status]}</Badge>
  );
  const payButton = (commission: Commission) =>
    commission.status !== "PAGO" && (
      <Button size="sm" variant="outline" onClick={() => onPay(commission.id)}>
        Pagar
      </Button>
    );

  return (
    <>
      {/* Celular: cards, para que valor, status e o botão Pagar fiquem sempre visíveis. */}
      <ul className="flex flex-col gap-3 sm:hidden">
        {commissions.map((commission) => (
          <li key={commission.id} className="flex flex-col gap-3 rounded-lg border border-border bg-card p-4 shadow-card">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-medium">{agentName(commission)}</p>
                <p className="text-sm text-muted-foreground tabular-nums">
                  {commission.rate}% de {formatCurrency(commission.baseValue)}
                </p>
              </div>
              {statusBadge(commission)}
            </div>
            <div className="flex items-center justify-between gap-3">
              <p className="text-lg font-semibold tabular-nums">{formatCurrency(commission.value)}</p>
              {payButton(commission)}
            </div>
          </li>
        ))}
      </ul>

      <div className="hidden overflow-x-auto rounded-lg border border-border bg-card shadow-card sm:block">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-secondary/50 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
              <th className="px-4 py-3">Corretor</th>
              <th className="px-4 py-3">Base</th>
              <th className="px-4 py-3">Taxa</th>
              <th className="px-4 py-3">Valor</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {commissions.map((commission) => (
              <tr key={commission.id} className="border-b border-border last:border-0 hover:bg-secondary/40">
                <td className="px-4 py-3 font-medium">{agentName(commission)}</td>
                <td className="px-4 py-3 tabular-nums">{formatCurrency(commission.baseValue)}</td>
                <td className="px-4 py-3 tabular-nums">{commission.rate}%</td>
                <td className="px-4 py-3 tabular-nums">{formatCurrency(commission.value)}</td>
                <td className="px-4 py-3">{statusBadge(commission)}</td>
                <td className="px-4 py-3 text-right">{payButton(commission)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
