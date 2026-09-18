import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/lib/utils";
import type { Commission } from "@/types/commission";

export function CommissionList({ commissions, onPay }: { commissions: Commission[]; onPay: (id: string) => void }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-sm">
        <thead>
          <tr className="border-b border-border text-left text-muted-foreground">
            <th className="py-2">Corretor</th>
            <th className="py-2">Base</th>
            <th className="py-2">Taxa</th>
            <th className="py-2">Valor</th>
            <th className="py-2">Status</th>
            <th className="py-2" />
          </tr>
        </thead>
        <tbody>
          {commissions.map((commission) => (
            <tr key={commission.id} className="border-b border-border">
              <td className="py-2">{commission.agentId.slice(0, 8)}</td>
              <td className="py-2">{formatCurrency(commission.baseValue)}</td>
              <td className="py-2">{commission.rate}%</td>
              <td className="py-2">{formatCurrency(commission.value)}</td>
              <td className="py-2">
                <Badge variant={commission.status === "PAGO" ? "success" : "secondary"}>{commission.status}</Badge>
              </td>
              <td className="py-2">
                {commission.status !== "PAGO" && (
                  <Button size="sm" variant="outline" onClick={() => onPay(commission.id)}>
                    Pagar
                  </Button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
