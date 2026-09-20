"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CurrencyInput, MaskedInput } from "@/components/ui/masked-input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { PropertySelect, UserSelect } from "@/components/shared/EntitySelects";
import { ApiRequestError } from "@/lib/api";
import { todayLocal } from "@/lib/utils";
import { documentError, formatCpfCnpj, onlyDigits } from "@/lib/masks";
import { useContractMutations } from "@/hooks/useContracts";
import type { ContractPrefill } from "@/lib/contracts";
import type { Contract, ContractRequest } from "@/types/contract";

const EMPTY: ContractRequest = {
  propertyId: "",
  agentId: "",
  type: "COMPRA_VENDA",
  value: 0,
  startDate: todayLocal(),
  buyerName: "",
  buyerDocument: "",
  ownerName: "",
  ownerDocument: "",
};

function withoutUndefined(prefill: ContractPrefill = {}): Partial<ContractRequest> {
  return Object.fromEntries(Object.entries(prefill).filter(([, v]) => v !== undefined));
}

export function ContractForm({ contract, prefill }: { contract?: Contract; prefill?: ContractPrefill }) {
  const router = useRouter();
  const { create, update } = useContractMutations();
  // Na edição (Admin/Financeiro) o backend devolve os documentos; o tipo de leitura os deixa opcionais.
  const [form, setForm] = useState<ContractRequest>(
    contract
      ? { ...contract, buyerDocument: onlyDigits(contract.buyerDocument ?? ""), ownerDocument: onlyDigits(contract.ownerDocument ?? "") }
      : { ...EMPTY, ...withoutUndefined(prefill) }
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const invalid = documentError(form.buyerDocument) ?? documentError(form.ownerDocument);
    if (invalid) {
      setError(invalid);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const saved = contract ? await update(contract.id, form) : await create(form);
      router.push(`/contratos/${saved.id}`);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Não foi possível salvar o contrato");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex max-w-xl flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="propertyId">Imóvel</Label>
          <PropertySelect id="propertyId" required onlyAvailable value={form.propertyId} onChange={(propertyId) => setForm((f) => ({ ...f, propertyId }))} />
        </div>
        <div>
          <Label htmlFor="agentId">Corretor</Label>
          <UserSelect id="agentId" required value={form.agentId} onChange={(agentId) => setForm((f) => ({ ...f, agentId }))} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="type">Tipo</Label>
          <Select
            id="type"
            className="w-full"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as ContractRequest["type"] })}
          >
            <option value="COMPRA_VENDA">Compra e venda</option>
            <option value="LOCACAO">Locação</option>
          </Select>
        </div>
        <div>
          <Label htmlFor="value">Valor</Label>
          <CurrencyInput id="value" required value={form.value} onValueChange={(value) => setForm({ ...form, value })} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="startDate">Data de início</Label>
          <Input id="startDate" type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
        </div>
        <div>
          <Label htmlFor="endDate">Data de término (locação)</Label>
          <Input id="endDate" type="date" value={form.endDate ?? ""} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="buyerName">Comprador/Locatário</Label>
          <Input id="buyerName" required value={form.buyerName} onChange={(e) => setForm({ ...form, buyerName: e.target.value })} />
        </div>
        <div>
          <Label htmlFor="buyerDocument">CPF/CNPJ</Label>
          <MaskedInput id="buyerDocument" required mask={formatCpfCnpj} validate={documentError} value={form.buyerDocument} onValueChange={(buyerDocument) => setForm({ ...form, buyerDocument })} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="ownerName">Proprietário</Label>
          <Input id="ownerName" required value={form.ownerName} onChange={(e) => setForm({ ...form, ownerName: e.target.value })} />
        </div>
        <div>
          <Label htmlFor="ownerDocument">CPF/CNPJ</Label>
          <MaskedInput id="ownerDocument" required mask={formatCpfCnpj} validate={documentError} value={form.ownerDocument} onValueChange={(ownerDocument) => setForm({ ...form, ownerDocument })} />
        </div>
      </div>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <Button type="submit" disabled={submitting}>
        {submitting ? "Salvando..." : "Salvar"}
      </Button>
    </form>
  );
}
