"use client";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { installmentCount } from "@/lib/contracts";
import { CONTRACT_TYPE_LABEL } from "@/lib/labels";
import { formatCurrency } from "@/lib/utils";
import type { Contract } from "@/types/contract";

// Resumo do que a ativação gera. A comissão não é prevista aqui: depende da taxa do corretor,
// que só o backend resolve (e só é visível ao ADMIN em /users).
export function ActivateContractDialog({
  contract,
  open,
  loading,
  onConfirm,
  onCancel,
}: {
  contract: Contract;
  open: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const parcelas = installmentCount(contract.type);

  return (
    <Dialog open={open} onOpenChange={(next) => !next && !loading && onCancel()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Ativar este contrato?</DialogTitle>
        </DialogHeader>
        <dl className="grid grid-cols-2 gap-2 text-sm">
          <dt className="text-muted-foreground">Tipo</dt>
          <dd>{CONTRACT_TYPE_LABEL[contract.type]}</dd>
          <dt className="text-muted-foreground">{contract.type === "LOCACAO" ? "Valor mensal" : "Valor"}</dt>
          <dd className="tabular-nums">{formatCurrency(contract.value)}</dd>
          <dt className="text-muted-foreground">Parcelas a gerar</dt>
          <dd>
            {parcelas} {parcelas === 1 ? "lançamento" : "parcelas mensais"}
          </dd>
        </dl>
        <ul className="mt-4 list-disc pl-5 text-sm text-muted-foreground">
          <li>O imóvel passa a {contract.type === "LOCACAO" ? "Alugado" : "Vendido"}.</li>
          <li>A comissão do corretor é calculada e registrada.</li>
        </ul>
        <p className="mt-3 text-sm font-medium">Esta ação é irreversível.</p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel} disabled={loading}>
            Cancelar
          </Button>
          <Button onClick={onConfirm} disabled={loading}>
            {loading ? "Ativando..." : "Ativar contrato"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
