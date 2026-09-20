"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { MaskedInput } from "@/components/ui/masked-input";
import { formatPhone } from "@/lib/masks";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { UserSelect } from "@/components/shared/EntitySelects";
import { ApiRequestError } from "@/lib/api";
import { useLeadMutations } from "@/hooks/useLeads";
import { LEAD_SOURCE_LABEL } from "@/lib/labels";
import type { LeadRequest, LeadSource } from "@/types/lead";

const EMPTY: LeadRequest = {
  name: "",
  phone: "",
  email: "",
  source: "SITE",
  assignedTo: "",
  notes: "",
};

export function LeadFormModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const { create } = useLeadMutations();
  const [form, setForm] = useState<LeadRequest>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleClose() {
    setForm(EMPTY);
    setError(null);
    onClose();
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await create(form);
      setForm(EMPTY);
      onCreated();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Não foi possível salvar o lead");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && handleClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Novo lead</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <Label htmlFor="name">Nome</Label>
            <Input id="name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="phone">Telefone</Label>
              <MaskedInput id="phone"required mask={formatPhone} value={form.phone} onValueChange={(phone) => setForm({ ...form, phone })} />
            </div>
            <div>
              <Label htmlFor="email">E-mail</Label>
              <Input id="email" type="email" value={form.email ?? ""} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="source">Origem</Label>
              <Select
                id="source"
                className="w-full"
                value={form.source}
                onChange={(e) => setForm({ ...form, source: e.target.value as LeadSource })}
              >
                {Object.entries(LEAD_SOURCE_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="assignedTo">Corretor</Label>
              <UserSelect
                id="assignedTo"
                required
                value={form.assignedTo}
                onChange={(assignedTo) => setForm((f) => ({ ...f, assignedTo }))}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="notes">Notas</Label>
            <textarea
              id="notes"
              rows={3}
              className="w-full rounded-md border border-border bg-card px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
              value={form.notes ?? ""}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
            />
          </div>

          {error && <p role="alert" className="text-sm text-danger">{error}</p>}

          <Button type="submit" disabled={submitting}>
            {submitting ? "Salvando..." : "Salvar"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
