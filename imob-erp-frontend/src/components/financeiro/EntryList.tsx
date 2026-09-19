import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ENTRY_CATEGORY_LABEL, ENTRY_STATUS_LABEL } from "@/lib/labels";
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
  const isOpen = (entry: FinancialEntry) => entry.status === "PENDENTE" || entry.status === "ATRASADO";

  const actions = (entry: FinancialEntry) => (
    <div className="flex justify-end gap-2">
      <Button size="sm" variant="outline" onClick={() => onPay(entry.id)}>
        Pagar
      </Button>
      <Button size="sm" variant="ghost" onClick={() => onCancel(entry.id)}>
        Cancelar
      </Button>
    </div>
  );

  return (
    <>
      {/* Celular: cards, para que valor, status e ações fiquem sempre visíveis (sem rolagem lateral). */}
      <ul className="flex flex-col gap-3 sm:hidden">
        {entries.map((entry) => (
          <li
            key={entry.id}
            className={`flex flex-col gap-3 rounded-lg border border-border p-4 shadow-card ${entry.status === "ATRASADO" ? "bg-danger-soft/40" : "bg-card"}`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-medium">{entry.description}</p>
                <p className="text-sm text-muted-foreground">
                  {ENTRY_CATEGORY_LABEL[entry.category]} · vence {formatDate(entry.dueDate)}
                </p>
              </div>
              <Badge variant={STATUS_VARIANT[entry.status]}>{ENTRY_STATUS_LABEL[entry.status]}</Badge>
            </div>
            <div className="flex items-center justify-between gap-3">
              <p className="text-lg font-semibold tabular-nums">{formatCurrency(entry.value)}</p>
              {isOpen(entry) && actions(entry)}
            </div>
          </li>
        ))}
      </ul>

      <div className="hidden overflow-x-auto rounded-lg border border-border bg-card shadow-card sm:block">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-secondary/50 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
              <th className="px-4 py-3">Descrição</th>
              <th className="px-4 py-3">Categoria</th>
              <th className="px-4 py-3">Vencimento</th>
              <th className="px-4 py-3">Valor</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr
                key={entry.id}
                className={`border-b border-border last:border-0 hover:bg-secondary/40 ${entry.status === "ATRASADO" ? "bg-danger-soft/40" : ""}`}
              >
                <td className="px-4 py-3 font-medium">{entry.description}</td>
                <td className="px-4 py-3 text-muted-foreground">{ENTRY_CATEGORY_LABEL[entry.category]}</td>
                <td className="px-4 py-3 tabular-nums">{formatDate(entry.dueDate)}</td>
                <td className="px-4 py-3 tabular-nums">{formatCurrency(entry.value)}</td>
                <td className="px-4 py-3">
                  <Badge variant={STATUS_VARIANT[entry.status]}>{ENTRY_STATUS_LABEL[entry.status]}</Badge>
                </td>
                <td className="px-4 py-3">{isOpen(entry) ? actions(entry) : null}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
