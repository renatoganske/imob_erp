"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ContractList } from "@/components/contratos/ContractList";
import { useContracts } from "@/hooks/useContracts";

export default function ContractsPage() {
  const { data, loading } = useContracts();

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Contratos</h1>
        <Button asChild>
          <Link href="/contratos/novo">Novo contrato</Link>
        </Button>
      </div>
      {loading && <p className="text-muted-foreground">Carregando...</p>}
      {data && <ContractList contracts={data.content} />}
    </div>
  );
}
