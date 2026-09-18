"use client";

import { Input } from "@/components/ui/input";
import type { PropertyFilters as Filters, PropertyStatus, PropertyType } from "@/types/property";

const TYPES: PropertyType[] = ["CASA", "APARTAMENTO", "COMERCIAL", "TERRENO"];
const STATUSES: PropertyStatus[] = ["DISPONIVEL", "RESERVADO", "VENDIDO", "ALUGADO"];

export function PropertyFilters({
  filters,
  onChange,
}: {
  filters: Filters;
  onChange: (filters: Filters) => void;
}) {
  return (
    <div className="flex flex-wrap gap-3">
      <select
        className="h-10 rounded-md border border-border bg-background px-3 text-sm"
        value={filters.type ?? ""}
        onChange={(e) => onChange({ ...filters, type: (e.target.value || undefined) as PropertyType | undefined })}
      >
        <option value="">Todos os tipos</option>
        {TYPES.map((type) => (
          <option key={type} value={type}>
            {type}
          </option>
        ))}
      </select>

      <select
        className="h-10 rounded-md border border-border bg-background px-3 text-sm"
        value={filters.status ?? ""}
        onChange={(e) => onChange({ ...filters, status: (e.target.value || undefined) as PropertyStatus | undefined })}
      >
        <option value="">Todos os status</option>
        {STATUSES.map((status) => (
          <option key={status} value={status}>
            {status}
          </option>
        ))}
      </select>

      <Input
        placeholder="Bairro"
        className="w-40"
        value={filters.neighborhood ?? ""}
        onChange={(e) => onChange({ ...filters, neighborhood: e.target.value || undefined })}
      />

      <Input
        type="number"
        placeholder="Preço mín."
        className="w-32"
        value={filters.minPrice ?? ""}
        onChange={(e) => onChange({ ...filters, minPrice: e.target.value ? Number(e.target.value) : undefined })}
      />

      <Input
        type="number"
        placeholder="Preço máx."
        className="w-32"
        value={filters.maxPrice ?? ""}
        onChange={(e) => onChange({ ...filters, maxPrice: e.target.value ? Number(e.target.value) : undefined })}
      />
    </div>
  );
}
