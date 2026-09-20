import Link from "next/link";
import { CONTRACT_TYPE_LABEL } from "@/lib/labels";
import { formatCurrency, formatDate, todayLocal } from "@/lib/utils";
import type { Contract } from "@/types/contract";
import { ContractStatusBadge } from "./ContractStatusBadge";
import { ExpiryBadge } from "./ExpiryBadge";

export function ContractList({ contracts, today = todayLocal() }: { contracts: Contract[]; today?: string }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-card shadow-card">
      <table className="w-full min-w-[320px] text-sm">
        <thead>
          <tr className="border-b border-border bg-secondary/50 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
            <th className="px-4 py-3">Tipo</th>
            <th className="px-4 py-3">Valor</th>
            <th className="hidden px-4 py-3 sm:table-cell">Início</th>
            <th className="px-4 py-3">Status</th>
          </tr>
        </thead>
        <tbody>
          {contracts.map((contract) => (
            <tr key={contract.id} className="border-b border-border last:border-0 hover:bg-secondary/40">
              <td className="px-4 py-3">
                <Link href={`/contratos/${contract.id}`} className="font-medium text-primary hover:underline">
                  {CONTRACT_TYPE_LABEL[contract.type]}
                </Link>
              </td>
              <td className="px-4 py-3 tabular-nums">{formatCurrency(contract.value)}</td>
              <td className="hidden px-4 py-3 tabular-nums sm:table-cell">{formatDate(contract.startDate)}</td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap items-center gap-1.5">
                  <ContractStatusBadge status={contract.status} />
                  <ExpiryBadge contract={contract} today={today} />
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
