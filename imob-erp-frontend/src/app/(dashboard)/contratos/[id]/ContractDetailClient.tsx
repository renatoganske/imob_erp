"use client";

import { useAuth } from "@clerk/nextjs";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ContractStatusBadge } from "@/components/contratos/ContractStatusBadge";
import { useContractMutations } from "@/hooks/useContracts";
import { ApiRequestError, api } from "@/lib/api";
import { CONTRACT_TYPE_LABEL } from "@/lib/labels";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { Contract } from "@/types/contract";

export function ContractDetailClient({ id }: { id: string }) {
  const { getToken } = useAuth();
  const { updateStatus } = useContractMutations();
  const [contract, setContract] = useState<Contract | null>(null);
  const [error, setError] = useState<{ message: string; code?: string } | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [activating, setActivating] = useState(false);

  function load() {
    api.get<Contract>(`/api/v1/contracts/${id}`, { getToken }).then(setContract);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function handleActivate() {
    if (!contract) return;
    setError(null);
    setActivating(true);
    try {
      await updateStatus(contract.id, "ATIVO");
      load();
    } catch (err) {
      setError(
        err instanceof ApiRequestError
          ? { message: err.message, code: err.code }
          : { message: "Não foi possível ativar o contrato" }
      );
    } finally {
      setActivating(false);
      setConfirming(false);
    }
  }

  if (!contract) return <p className="text-muted-foreground">Carregando...</p>;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight sm:text-2xl">{CONTRACT_TYPE_LABEL[contract.type]}</h1>
          <p className="text-muted-foreground">{formatCurrency(contract.value)} — início em {formatDate(contract.startDate)}</p>
        </div>
        <div className="flex items-center gap-3">
          <ContractStatusBadge status={contract.status} />
          {contract.status === "RASCUNHO" && <Button onClick={() => setConfirming(true)}>Ativar contrato</Button>}
        </div>
      </div>

      {error && (
        <p role="alert" className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
          {error.message}{" "}
          {error.code === "AGENT_WITHOUT_COMMISSION_RATE" && (
            <Link href="/configuracoes/usuarios" className="font-medium underline">
              Ir para Usuários
            </Link>
          )}
        </p>
      )}

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
      <ConfirmDialog
        open={confirming}
        title="Ativar este contrato?"
        description="A ação é irreversível: o imóvel muda de status e são geradas as parcelas financeiras e a comissão do corretor."
        confirmLabel="Ativar contrato"
        loading={activating}
        onConfirm={handleActivate}
        onCancel={() => setConfirming(false)}
      />
    </div>
  );
}
