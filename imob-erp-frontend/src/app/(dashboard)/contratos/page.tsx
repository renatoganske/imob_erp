"use client";

import Link from "next/link";
import { FileText, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState, ListSkeleton } from "@/components/ui/state";
import { ContractList } from "@/components/contratos/ContractList";
import { useContracts } from "@/hooks/useContracts";

export default function ContractsPage() {
  const { data, loading } = useContracts();

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
      {loading && <ListSkeleton />}
      {data && data.content.length === 0 && (
        <EmptyState icon={FileText} title="Nenhum contrato ainda" description="Crie o primeiro contrato a partir de um imóvel e um cliente." />
      )}
      {data && data.content.length > 0 && <ContractList contracts={data.content} />}
    </div>
  );
}
