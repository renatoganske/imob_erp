"use client";

import { useAuth } from "@clerk/nextjs";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";
import type { AppUser, UserInviteRequest } from "@/types/user";

export default function UsersPage() {
  const { getToken } = useAuth();
  const [users, setUsers] = useState<AppUser[]>([]);
  const [form, setForm] = useState<UserInviteRequest>({ name: "", email: "", role: "CORRETOR" });

  function load() {
    api.get<AppUser[]>("/api/v1/users", { getToken }).then(setUsers);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault();
    await api.post("/api/v1/users/invite", form, { getToken });
    setForm({ name: "", email: "", role: "CORRETOR" });
    load();
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Usuários</h1>

      <form onSubmit={handleInvite} className="flex flex-wrap items-end gap-3">
        <Input placeholder="Nome" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <Input placeholder="E-mail" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <select
          className="h-10 rounded-md border border-border bg-background px-3 text-sm"
          value={form.role}
          onChange={(e) => setForm({ ...form, role: e.target.value as UserInviteRequest["role"] })}
        >
          <option value="ADMIN">Admin</option>
          <option value="CORRETOR">Corretor</option>
          <option value="FINANCEIRO">Financeiro</option>
        </select>
        <Button type="submit">Convidar</Button>
      </form>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[480px] text-sm">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="py-2">Nome</th>
              <th className="py-2">E-mail</th>
              <th className="py-2">Papel</th>
              <th className="py-2">Status</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-b border-border">
                <td className="py-2">{user.name}</td>
                <td className="py-2">{user.email}</td>
                <td className="py-2">{user.role}</td>
                <td className="py-2">
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
