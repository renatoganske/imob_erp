import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { FinancialEntry, FinancialStatus } from "@/types/financial";

const STATUS_VARIANT: Record<FinancialStatus, "success" | "warning" | "destructive" | "secondary"> = {
  PAGO: "success",
  PENDENTE: "secondary",
  ATRASADO: "destructive",
  CANCELADO: "warning",
};

export function EntryList({
  entries,
  onPay,
  onCancel,
}: {
  entries: FinancialEntry[];
  onPay: (id: string) => void;
  onCancel: (id: string) => void;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-sm">
        <thead>
          <tr className="border-b border-border text-left text-muted-foreground">
            <th className="py-2">Descrição</th>
            <th className="py-2">Categoria</th>
            <th className="py-2">Vencimento</th>
            <th className="py-2">Valor</th>
            <th className="py-2">Status</th>
            <th className="py-2" />
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id} className={`border-b border-border ${entry.status === "ATRASADO" ? "bg-destructive/5" : ""}`}>
              <td className="py-2">{entry.description}</td>
              <td className="py-2">{entry.category}</td>
              <td className="py-2">{formatDate(entry.dueDate)}</td>
              <td className="py-2">{formatCurrency(entry.value)}</td>
              <td className="py-2">
                <Badge variant={STATUS_VARIANT[entry.status]}>{entry.status}</Badge>
              </td>
              <td className="py-2">
                {entry.status === "PENDENTE" || entry.status === "ATRASADO" ? (
                  <div className="flex gap-2">
                    <Button size="sm" variant="outline" onClick={() => onPay(entry.id)}>
                      Pagar
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => onCancel(entry.id)}>
                      Cancelar
                    </Button>
                  </div>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
