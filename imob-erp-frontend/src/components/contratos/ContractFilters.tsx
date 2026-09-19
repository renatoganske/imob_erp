"use client";

import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import type { ContractFilterValues } from "@/lib/contracts";
import { CONTRACT_STATUS_LABEL, CONTRACT_TYPE_LABEL } from "@/lib/labels";
import type { ContractStatus, ContractType } from "@/types/contract";

export function ContractFilters({
  filters,
  onChange,
}: {
  filters: ContractFilterValues;
  onChange: (filters: ContractFilterValues) => void;
}) {
  return (
    <div className="flex flex-wrap items-end gap-3" role="search" aria-label="Filtrar contratos">
      <Select
        aria-label="Status"
        value={filters.status ?? ""}
        onChange={(e) => onChange({ ...filters, status: (e.target.value || undefined) as ContractStatus | undefined })}
      >
        <option value="">Todos os status</option>
        {(Object.keys(CONTRACT_STATUS_LABEL) as ContractStatus[]).map((s) => (
          <option key={s} value={s}>
            {CONTRACT_STATUS_LABEL[s]}
          </option>
        ))}
      </Select>
      <Select
        aria-label="Tipo"
        value={filters.type ?? ""}
        onChange={(e) => onChange({ ...filters, type: (e.target.value || undefined) as ContractType | undefined })}
      >
        <option value="">Compra e locação</option>
        {(Object.keys(CONTRACT_TYPE_LABEL) as ContractType[]).map((t) => (
          <option key={t} value={t}>
            {CONTRACT_TYPE_LABEL[t]}
          </option>
        ))}
      </Select>
      <label className="flex flex-col gap-1 text-xs text-muted-foreground">
        Início de
        <Input
          type="date"
          className="w-40"
          value={filters.from ?? ""}
          onChange={(e) => onChange({ ...filters, from: e.target.value || undefined })}
        />
      </label>
      <label className="flex flex-col gap-1 text-xs text-muted-foreground">
        Início até
        <Input
          type="date"
          className="w-40"
          value={filters.to ?? ""}
          onChange={(e) => onChange({ ...filters, to: e.target.value || undefined })}
        />
      </label>
    </div>
  );
}
