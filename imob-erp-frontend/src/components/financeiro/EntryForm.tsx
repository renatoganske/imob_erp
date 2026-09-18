"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useFinancialMutations } from "@/hooks/useFinancial";
import type { FinancialEntryRequest } from "@/types/financial";

const EMPTY: FinancialEntryRequest = {
  type: "RECEITA",
  category: "OUTRO",
  description: "",
  value: 0,
  dueDate: new Date().toISOString().slice(0, 10),
};

export function EntryForm({ onCreated }: { onCreated: () => void }) {
  const { create } = useFinancialMutations();
  const [form, setForm] = useState<FinancialEntryRequest>(EMPTY);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await create(form);
      setForm(EMPTY);
      onCreated();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
      <div>
        <Label htmlFor="type">Tipo</Label>
        <select
          id="type"
          className="h-10 rounded-md border border-border bg-background px-3 text-sm"
          value={form.type}
          onChange={(e) => setForm({ ...form, type: e.target.value as FinancialEntryRequest["type"] })}
        >
          <option value="RECEITA">Receita</option>
          <option value="DESPESA">Despesa</option>
        </select>
      </div>
      <div>
        <Label htmlFor="description">Descrição</Label>
        <Input id="description" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
      </div>
      <div>
        <Label htmlFor="value">Valor</Label>
        <Input id="value" type="number" required className="w-32" value={form.value} onChange={(e) => setForm({ ...form, value: Number(e.target.value) })} />
      </div>
      <div>
        <Label htmlFor="dueDate">Vencimento</Label>
        <Input id="dueDate" type="date" required value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
      </div>
      <Button type="submit" disabled={submitting}>
        {submitting ? "Salvando..." : "Lançar"}
      </Button>
    </form>
  );
}
