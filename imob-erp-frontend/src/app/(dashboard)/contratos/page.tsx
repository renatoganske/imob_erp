"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { FileText, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { ContractFilters } from "@/components/contratos/ContractFilters";
import { ContractList } from "@/components/contratos/ContractList";
import { useContracts } from "@/hooks/useContracts";
import { CONTRACTS_PAGE_SIZE, filterByPeriod, type ContractFilterValues } from "@/lib/contracts";

export default function ContractsPage() {
  const [filters, setFilters] = useState<ContractFilterValues>({});
  const { data, loading } = useContracts({ status: filters.status, type: filters.type, size: CONTRACTS_PAGE_SIZE });
  const contracts = useMemo(() => filterByPeriod(data?.content ?? [], filters.from, filters.to), [data, filters.from, filters.to]);
  const filtering = Boolean(filters.status || filters.type || filters.from || filters.to);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Contratos"
        description="Vendas e locações em andamento e encerradas."
        actions={
          <Button asChild>
            <Link href="/contratos/novo">
              <Plus className="h-4 w-4" />
              Novo contrato
            </Link>
          </Button>
        }
      />
      <ContractFilters filters={filters} onChange={setFilters} />
      {loading && <ListSkeleton />}
      {data && contracts.length === 0 && (
        <EmptyState
          icon={FileText}
          title={filtering ? "Nenhum contrato encontrado" : "Nenhum contrato ainda"}
          description={filtering ? "Ajuste os filtros para ver outros contratos." : "Crie o primeiro contrato a partir de um imóvel e um cliente."}
        />
      )}
      {data && contracts.length > 0 && <ContractList contracts={contracts} />}
    </div>
  );
}
