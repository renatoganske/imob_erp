"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { CurrencyInput } from "@/components/ui/masked-input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { ApiRequestError } from "@/lib/api";
import { useFinancialMutations } from "@/hooks/useFinancial";
import { ENTRY_CATEGORY_LABEL } from "@/lib/labels";
import { todayLocal } from "@/lib/utils";
import type { FinancialCategory, FinancialType } from "@/types/financial";

const CATEGORIES: Record<FinancialType, FinancialCategory[]> = {
  RECEITA: ["ALUGUEL", "PARCELA_VENDA", "TAXA_ADMINISTRACAO", "OUTRO"],
  DESPESA: ["REPASSE_PROPRIETARIO", "COMISSAO", "DESPESA_OPERACIONAL", "OUTRO"],
};

// O tipo vem da tela (receber = receita, pagar = despesa): não é uma escolha do usuário.
export function EntryForm({ type, onCreated }: { type: FinancialType; onCreated: () => void }) {
  const { create } = useFinancialMutations();
  const empty = () => ({ category: "OUTRO" as FinancialCategory, description: "", value: 0, dueDate: todayLocal() });
  const [form, setForm] = useState(empty);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await create({ type, category: form.category, description: form.description, value: form.value, dueDate: form.dueDate });
      setForm(empty());
      onCreated();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Não foi possível salvar o lançamento");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="p-4">
      <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
        <div className="flex min-w-56 flex-1 flex-col gap-1.5">
          <Label htmlFor="description">Descrição</Label>
          <Input id="description" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="category">Categoria</Label>
          <Select
            id="category"
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value as FinancialCategory })}
          >
            {CATEGORIES[type].map((category) => (
              <option key={category} value={category}>
                {ENTRY_CATEGORY_LABEL[category]}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="value">Valor (R$)</Label>
          <CurrencyInput id="value" required className="w-40" value={form.value} onValueChange={(value) => setForm({ ...form, value })} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="dueDate">Vencimento</Label>
          <Input id="dueDate" type="date" required value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
        </div>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Salvando..." : "Lançar"}
        </Button>
      </form>
      {error && (
        <p role="alert" className="mt-3 text-sm text-danger">
          {error}
        </p>
      )}
    </Card>
  );
}
