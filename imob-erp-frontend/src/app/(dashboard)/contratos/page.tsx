"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { FileText, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { ContractFilters } from "@/components/contratos/ContractFilters";
import { ContractList } from "@/components/contratos/ContractList";
import { ExpiringContractsAlerts } from "@/components/contratos/ExpiringContractsAlerts";
import { useContracts, useExpiringContractsSummary } from "@/hooks/useContracts";
import { useRole } from "@/hooks/useRole";
import { canManageContracts } from "@/lib/permissions";
import {
  CONTRACTS_PAGE_SIZE,
  filterByPeriod,
  parseExpiringInDays,
  type ContractFilterValues,
} from "@/lib/contracts";

function ContractsContent() {
  const router = useRouter();
  const canManage = canManageContracts(useRole());
  const expiringInDays = parseExpiringInDays(useSearchParams().get("expiringInDays"));
  const [filters, setFilters] = useState<ContractFilterValues>({});
  // O filtro de vencimento já implica locação ativa: o backend recusa combiná-lo com outro status/tipo.
  const { data, loading } = useContracts({
    ...(expiringInDays ? { expiringInDays } : { status: filters.status, type: filters.type }),
    size: CONTRACTS_PAGE_SIZE,
  });
  const { data: expiring } = useExpiringContractsSummary(canManage);
  const contracts = useMemo(() => filterByPeriod(data?.content ?? [], filters.from, filters.to), [data, filters.from, filters.to]);
  const filtering = Boolean(expiringInDays || filters.status || filters.type || filters.from || filters.to);

  // Escolher status/tipo tira a tela do modo "vencendo em breve".
  const changeFilters = (next: ContractFilterValues) => {
    if (expiringInDays) router.replace("/contratos");
    setFilters(next);
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Contratos"
        description={
          canManage ? "Vendas e locações em andamento e encerradas." : "Os contratos em que você é o corretor responsável."
        }
        actions={
          canManage ? (
            <Button asChild>
              <Link href="/contratos/novo">
                <Plus className="h-4 w-4" />
                Novo contrato
              </Link>
            </Button>
          ) : undefined
        }
      />
      {expiring && <ExpiringContractsAlerts summary={expiring} activeWindow={expiringInDays} />}
      {expiringInDays && (
        <p className="flex flex-wrap items-center gap-2 text-sm">
          Mostrando locações ativas que vencem em até {expiringInDays} dias, das mais próximas às mais distantes.
          <Link href="/contratos" className="font-medium text-primary hover:underline">
            Limpar filtro
          </Link>
        </p>
      )}
      <ContractFilters filters={filters} onChange={changeFilters} />
      {loading && <ListSkeleton />}
      {data && contracts.length === 0 && (
        <EmptyState
          icon={FileText}
          title={filtering ? "Nenhum contrato encontrado" : "Nenhum contrato ainda"}
          description={
            filtering
              ? "Ajuste os filtros para ver outros contratos."
              : canManage
                ? "Crie o primeiro contrato a partir de um imóvel e um cliente."
                : "Quando um lead seu for fechado, o contrato aparece aqui."
          }
        />
      )}
      {data && contracts.length > 0 && <ContractList contracts={contracts} />}
    </div>
  );
}

// useSearchParams exige Suspense no build de produção do Next.
export default function ContractsPage() {
  return (
    <Suspense fallback={<ListSkeleton />}>
      <ContractsContent />
    </Suspense>
  );
}
