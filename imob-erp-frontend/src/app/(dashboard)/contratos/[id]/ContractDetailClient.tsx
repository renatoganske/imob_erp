"use client";

import { useAuth } from "@clerk/nextjs";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { ContractStatusBadge } from "@/components/contratos/ContractStatusBadge";
import { useContractMutations } from "@/hooks/useContracts";
import { api } from "@/lib/api";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { Contract } from "@/types/contract";

export function ContractDetailClient({ id }: { id: string }) {
  const { getToken } = useAuth();
  const { updateStatus } = useContractMutations();
  const [contract, setContract] = useState<Contract | null>(null);

  function load() {
    api.get<Contract>(`/api/v1/contracts/${id}`, { getToken }).then(setContract);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function handleActivate() {
    if (!contract) return;
    if (!confirm("Ativar este contrato? A ação é irreversível e gera parcelas financeiras e comissão.")) return;
    await updateStatus(contract.id, "ATIVO");
    load();
  }

  if (!contract) return <p className="text-muted-foreground">Carregando...</p>;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{contract.type}</h1>
          <p className="text-muted-foreground">{formatCurrency(contract.value)} — início em {formatDate(contract.startDate)}</p>
        </div>
        <div className="flex items-center gap-3">
          <ContractStatusBadge status={contract.status} />
          {contract.status === "RASCUNHO" && <Button onClick={handleActivate}>Ativar contrato</Button>}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
        <div>
          <p className="text-muted-foreground">Comprador/Locatário</p>
          <p>{contract.buyerName} — {contract.buyerDocument}</p>
        </div>
        <div>
          <p className="text-muted-foreground">Proprietário</p>
          <p>{contract.ownerName} — {contract.ownerDocument}</p>
        </div>
      </div>
    </div>
  );
}
