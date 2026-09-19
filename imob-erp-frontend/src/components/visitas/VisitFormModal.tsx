"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { LeadSelect, PropertySelect, UserSelect } from "@/components/shared/EntitySelects";
import { ApiRequestError } from "@/lib/api";
import { useVisitMutations } from "@/hooks/useVisits";

const EMPTY = { leadId: "", propertyId: "", agentId: "", scheduledAt: "" };

export function VisitFormModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const { create } = useVisitMutations();
  const [form, setForm] = useState(EMPTY);
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
      // datetime-local não tem fuso: converte do horário local para ISO com offset.
      await create({ ...form, scheduledAt: new Date(form.scheduledAt).toISOString() });
      setForm(EMPTY);
      onCreated();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Não foi possível agendar a visita");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && handleClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Agendar visita</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="visit-lead">Lead</Label>
            <LeadSelect id="visit-lead" required value={form.leadId} onChange={(leadId) => setForm((f) => ({ ...f, leadId }))} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="visit-property">Imóvel</Label>
            <PropertySelect
              id="visit-property"
              required
              value={form.propertyId}
              onChange={(propertyId) => setForm((f) => ({ ...f, propertyId }))}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="visit-agent">Corretor</Label>
              <UserSelect id="visit-agent" required value={form.agentId} onChange={(agentId) => setForm((f) => ({ ...f, agentId }))} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="visit-when">Data e hora</Label>
              <Input
                id="visit-when"
                type="datetime-local"
                required
                value={form.scheduledAt}
                onChange={(e) => setForm((f) => ({ ...f, scheduledAt: e.target.value }))}
              />
            </div>
          </div>

          {error && (
            <p role="alert" className="text-sm text-danger">
              {error}
            </p>
          )}

          <Button type="submit" disabled={submitting}>
            {submitting ? "Agendando..." : "Agendar"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
