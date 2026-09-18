"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api";
import { useLeadMutations } from "@/hooks/useLeads";
import type { LeadRequest, LeadSource } from "@/types/lead";

const EMPTY: LeadRequest = {
  name: "",
  phone: "",
  email: "",
  source: "SITE",
  assignedTo: "",
  notes: "",
};

const SOURCE_LABEL: Record<LeadSource, string> = {
  WHATSAPP: "WhatsApp",
  SITE: "Site",
  INDICACAO: "Indicação",
  PORTAL_ZAP: "Portal Zap",
  PORTAL_VIVAREAL: "Portal VivaReal",
  OUTRO: "Outro",
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
      setError(err instanceof ApiRequestError ? err.message : "Nao foi possivel salvar o lead");
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
              <Input id="phone" required value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
            <div>
              <Label htmlFor="email">E-mail</Label>
              <Input id="email" type="email" value={form.email ?? ""} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="source">Origem</Label>
              <select
                id="source"
                className="h-10 w-full rounded-md border border-border bg-background px-3 text-sm"
                value={form.source}
                onChange={(e) => setForm({ ...form, source: e.target.value as LeadSource })}
              >
                {Object.entries(SOURCE_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Label htmlFor="assignedTo">Corretor (ID)</Label>
              <Input
                id="assignedTo"
                required
                value={form.assignedTo}
                onChange={(e) => setForm({ ...form, assignedTo: e.target.value })}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="notes">Notas</Label>
            <textarea
              id="notes"
              rows={3}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
              value={form.notes ?? ""}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <Button type="submit" disabled={submitting}>
            {submitting ? "Salvando..." : "Salvar"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
