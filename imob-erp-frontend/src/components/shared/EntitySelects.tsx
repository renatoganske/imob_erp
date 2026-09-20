"use client";

import { useEffect } from "react";
import { Select } from "@/components/ui/select";
import { useLeads } from "@/hooks/useLeads";
import { useProperties } from "@/hooks/useProperties";
import { useUsers } from "@/hooks/useUsers";
import { formatPhone } from "@/lib/masks";
import { formatCurrency } from "@/lib/utils";

interface SelectProps {
  id: string;
  onlyAvailable?: boolean;
  value: string;
  onChange: (id: string) => void;
  required?: boolean;
}

const FULL = "w-full";

export function UserSelect({ id, value, onChange, required }: SelectProps) {
  const { data: users, loading } = useUsers();
  const active = users.filter((u) => u.active);

  // Com um único corretor não há o que escolher: já vem selecionado.
  useEffect(() => {
    if (!value && active.length === 1) onChange(active[0].id);
  }, [value, active, onChange]);

  return (
    <Select id={id} required={required} className={FULL} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{loading ? "Carregando..." : "Selecione o corretor"}</option>
      {active.map((user) => (
        <option key={user.id} value={user.id}>
          {user.name}
        </option>
      ))}
    </Select>
  );
}

export function PropertySelect({ id, value, onChange, required, onlyAvailable }: SelectProps) {
  const { data, loading } = useProperties(onlyAvailable ? { status: "DISPONIVEL" } : {});

  return (
    <Select id={id} required={required} className={FULL} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">
        {loading
          ? "Carregando..."
          : onlyAvailable && data?.content.length === 0
            ? "Nenhum imóvel disponível"
            : "Selecione o imóvel"}
      </option>
      {data?.content.map((property) => (
        <option key={property.id} value={property.id}>
          {property.title} — {formatCurrency(property.price)}
        </option>
      ))}
    </Select>
  );
}

export function LeadSelect({ id, value, onChange, required }: SelectProps) {
  const { data, loading } = useLeads();

  return (
    <Select id={id} required={required} className={FULL} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{loading ? "Carregando..." : "Selecione o lead"}</option>
      {data?.content.map((lead) => (
        <option key={lead.id} value={lead.id}>
          {lead.name} — {formatPhone(lead.phone)}
        </option>
      ))}
    </Select>
  );
}
