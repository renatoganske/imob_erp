"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useContractMutations } from "@/hooks/useContracts";
import type { Contract, ContractRequest } from "@/types/contract";

const EMPTY: ContractRequest = {
  propertyId: "",
  agentId: "",
  type: "COMPRA_VENDA",
  value: 0,
  startDate: new Date().toISOString().slice(0, 10),
  buyerName: "",
  buyerDocument: "",
  ownerName: "",
  ownerDocument: "",
};

export function ContractForm({ contract }: { contract?: Contract }) {
  const router = useRouter();
  const { create, update } = useContractMutations();
  const [form, setForm] = useState<ContractRequest>(contract ?? EMPTY);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const saved = contract ? await update(contract.id, form) : await create(form);
      router.push(`/contratos/${saved.id}`);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex max-w-xl flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="propertyId">Imóvel (ID)</Label>
          <Input id="propertyId" required value={form.propertyId} onChange={(e) => setForm({ ...form, propertyId: e.target.value })} />
        </div>
        <div>
          <Label htmlFor="agentId">Corretor (ID)</Label>
          <Input id="agentId" required value={form.agentId} onChange={(e) => setForm({ ...form, agentId: e.target.value })} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="type">Tipo</Label>
          <select
            id="type"
            className="h-10 w-full rounded-md border border-border bg-background px-3 text-sm"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as ContractRequest["type"] })}
          >
            <option value="COMPRA_VENDA">Compra e venda</option>
            <option value="LOCACAO">Locação</option>
          </select>
        </div>
        <div>
          <Label htmlFor="value">Valor</Label>
          <Input id="value" type="number" required value={form.value} onChange={(e) => setForm({ ...form, value: Number(e.target.value) })} />
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
          <Input id="buyerDocument" required value={form.buyerDocument} onChange={(e) => setForm({ ...form, buyerDocument: e.target.value })} />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="ownerName">Proprietário</Label>
          <Input id="ownerName" required value={form.ownerName} onChange={(e) => setForm({ ...form, ownerName: e.target.value })} />
        </div>
        <div>
          <Label htmlFor="ownerDocument">CPF/CNPJ</Label>
          <Input id="ownerDocument" required value={form.ownerDocument} onChange={(e) => setForm({ ...form, ownerDocument: e.target.value })} />
        </div>
      </div>

      <Button type="submit" disabled={submitting}>
        {submitting ? "Salvando..." : "Salvar"}
      </Button>
    </form>
  );
}
