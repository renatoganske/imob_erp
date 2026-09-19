"use client";

import { useAuth } from "@clerk/nextjs";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import { Select } from "@/components/ui/select";
import { ApiRequestError, api } from "@/lib/api";
import type { AppUser, UserInviteRequest } from "@/types/user";

const ROLE_LABEL: Record<AppUser["role"], string> = {
  ADMIN: "Administrador",
  CORRETOR: "Corretor",
  FINANCEIRO: "Financeiro",
};

function CommissionRateCell({ user, onSaved }: { user: AppUser; onSaved: () => void }) {
  const { getToken } = useAuth();
  const saved = user.commissionRate != null ? String(user.commissionRate) : "";
  const [value, setValue] = useState(saved);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setValue(saved);
  }, [saved]);

  const dirty = value !== saved && value !== "";

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      await api.patch(`/api/v1/users/${user.id}/commission-rate`, { commissionRate: Number(value) }, { getToken });
      onSaved();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Não foi possível salvar");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-2">
        <Input
          aria-label={`Taxa de comissão de ${user.name}`}
          type="number"
          min={0}
          max={100}
          step="0.01"
          placeholder="—"
          className="h-9 w-24"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && dirty && handleSave()}
        />
        <span className="text-muted-foreground">%</span>
        {dirty && (
          <Button size="sm" variant="soft" onClick={handleSave} disabled={saving}>
            {saving ? "Salvando..." : "Salvar"}
          </Button>
        )}
        {!dirty && user.commissionRate == null && <Badge variant="warning">Sem taxa</Badge>}
      </div>
      {error && (
        <p role="alert" className="text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  );
}

export default function UsersPage() {
  const { getToken } = useAuth();
  const [users, setUsers] = useState<AppUser[]>([]);
  const [form, setForm] = useState<UserInviteRequest>({ name: "", email: "", role: "CORRETOR" });
  const [inviteError, setInviteError] = useState<string | null>(null);

  function load() {
    api.get<AppUser[]>("/api/v1/users", { getToken }).then(setUsers);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault();
    setInviteError(null);
    try {
      await api.post("/api/v1/users/invite", form, { getToken });
      setForm({ name: "", email: "", role: "CORRETOR" });
      load();
    } catch (err) {
      setInviteError(err instanceof ApiRequestError ? err.message : "Não foi possível enviar o convite");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Usuários"
        description="Convide a equipe e defina a taxa de comissão de cada corretor. Contratos só podem ser ativados quando o corretor tem taxa definida."
      />

      <Card className="p-4">
        <form onSubmit={handleInvite} className="flex flex-wrap items-end gap-3">
          <div className="flex min-w-48 flex-1 flex-col gap-1.5">
            <Label htmlFor="invite-name">Nome</Label>
            <Input id="invite-name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div className="flex min-w-56 flex-1 flex-col gap-1.5">
            <Label htmlFor="invite-email">E-mail</Label>
            <Input id="invite-email" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="invite-role">Papel</Label>
            <Select
              id="invite-role"
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value as UserInviteRequest["role"] })}
            >
              <option value="ADMIN">Administrador</option>
              <option value="CORRETOR">Corretor</option>
              <option value="FINANCEIRO">Financeiro</option>
            </Select>
          </div>
          <Button type="submit">Convidar</Button>
        </form>
        {inviteError && (
          <p role="alert" className="mt-3 text-sm text-danger">
            {inviteError}
          </p>
        )}
      </Card>

      <div className="overflow-x-auto rounded-lg border border-border bg-card shadow-card">
        <table className="w-full min-w-[640px] text-sm">
          <thead>
            <tr className="border-b border-border bg-secondary/50 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">E-mail</th>
              <th className="px-4 py-3">Papel</th>
              <th className="px-4 py-3">Comissão</th>
              <th className="px-4 py-3">Status</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-b border-border last:border-0">
                <td className="px-4 py-3 font-medium">{user.name}</td>
                <td className="px-4 py-3 text-muted-foreground">{user.email}</td>
                <td className="px-4 py-3">{ROLE_LABEL[user.role]}</td>
                <td className="px-4 py-3">
                  <CommissionRateCell user={user} onSaved={load} />
                </td>
                <td className="px-4 py-3">
                  <Badge variant={user.active ? "success" : "secondary"}>{user.active ? "Ativo" : "Pendente"}</Badge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
