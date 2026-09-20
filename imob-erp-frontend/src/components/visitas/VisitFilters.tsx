"use client";

import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { VISIT_STATUS_LABEL } from "@/lib/labels";
import type { VisitFilterValues } from "@/lib/visits";
import type { VisitStatus } from "@/types/visit";

export function VisitFilters({
  filters,
  onChange,
}: {
  filters: VisitFilterValues;
  onChange: (filters: VisitFilterValues) => void;
}) {
  return (
    <div className="flex flex-wrap items-end gap-3" role="search" aria-label="Filtrar visitas">
      <Select
        aria-label="Status"
        value={filters.status ?? ""}
        onChange={(e) => onChange({ ...filters, status: (e.target.value || undefined) as VisitStatus | undefined })}
      >
        <option value="">Todos os status</option>
        {(Object.keys(VISIT_STATUS_LABEL) as VisitStatus[]).map((s) => (
          <option key={s} value={s}>
            {VISIT_STATUS_LABEL[s]}
          </option>
        ))}
      </Select>
      <label className="flex flex-col gap-1 text-xs text-muted-foreground">
        De
        <Input
          type="date"
          className="w-40"
          value={filters.from ?? ""}
          onChange={(e) => onChange({ ...filters, from: e.target.value || undefined })}
        />
      </label>
      <label className="flex flex-col gap-1 text-xs text-muted-foreground">
        Até
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
