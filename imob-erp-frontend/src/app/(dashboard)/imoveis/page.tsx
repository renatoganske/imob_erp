"use client";

import { useState } from "react";
import Link from "next/link";
import { Building2, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { CardGridSkeleton, EmptyState } from "@/components/ui/state";
import { PropertyCard } from "@/components/imoveis/PropertyCard";
import { PropertyFilters } from "@/components/imoveis/PropertyFilters";
import { useProperties } from "@/hooks/useProperties";
import type { PropertyFilters as Filters } from "@/types/property";

export default function PropertiesPage() {
  const [filters, setFilters] = useState<Filters>({});
  const { data, loading } = useProperties(filters);
  const hasFilters = Object.values(filters).some((v) => v !== undefined);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Imóveis"
        description="Portfólio de imóveis da imobiliária."
        actions={
          <Button asChild>
            <Link href="/imoveis/novo">
              <Plus className="h-4 w-4" />
              Novo imóvel
            </Link>
          </Button>
        }
      />

      <PropertyFilters filters={filters} onChange={setFilters} />

      {loading && <CardGridSkeleton />}

      {!loading && data && data.content.length === 0 && (
        <EmptyState
          icon={Building2}
          title={hasFilters ? "Nenhum imóvel com esses filtros" : "Nenhum imóvel cadastrado"}
          description={hasFilters ? "Ajuste ou limpe os filtros para ver mais resultados." : "Cadastre o primeiro imóvel para começar."}
          action={
            hasFilters ? (
              <Button variant="outline" onClick={() => setFilters({})}>
                Limpar filtros
              </Button>
            ) : (
              <Button asChild>
                <Link href="/imoveis/novo">Novo imóvel</Link>
              </Button>
            )
          }
        />
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.content.map((property) => (
          <PropertyCard key={property.id} property={property} />
        ))}
      </div>
    </div>
  );
}
