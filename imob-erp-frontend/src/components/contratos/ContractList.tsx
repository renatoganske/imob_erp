import Link from "next/link";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { Contract } from "@/types/contract";
import { ContractStatusBadge } from "./ContractStatusBadge";

export function ContractList({ contracts }: { contracts: Contract[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[480px] text-sm">
        <thead>
          <tr className="border-b border-border text-left text-muted-foreground">
            <th className="py-2">Tipo</th>
            <th className="py-2">Valor</th>
            <th className="py-2">Início</th>
            <th className="py-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {contracts.map((contract) => (
            <tr key={contract.id} className="border-b border-border">
              <td className="py-2">
                <Link href={`/contratos/${contract.id}`} className="hover:underline">
                  {contract.type}
                </Link>
              </td>
              <td className="py-2">{formatCurrency(contract.value)}</td>
              <td className="py-2">{formatDate(contract.startDate)}</td>
              <td className="py-2">
                <ContractStatusBadge status={contract.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
