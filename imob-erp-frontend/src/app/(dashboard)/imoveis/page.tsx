"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { PropertyCard } from "@/components/imoveis/PropertyCard";
import { PropertyFilters } from "@/components/imoveis/PropertyFilters";
import { useProperties } from "@/hooks/useProperties";
import type { PropertyFilters as Filters } from "@/types/property";

export default function PropertiesPage() {
  const [filters, setFilters] = useState<Filters>({});
  const { data, loading } = useProperties(filters);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Imóveis</h1>
        <Button asChild>
          <Link href="/imoveis/novo">Novo imóvel</Link>
        </Button>
      </div>

      <PropertyFilters filters={filters} onChange={setFilters} />

      {loading && <p className="text-muted-foreground">Carregando...</p>}

      {!loading && data && data.content.length === 0 && (
        <p className="text-muted-foreground">Nenhum imóvel encontrado.</p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.content.map((property) => (
          <PropertyCard key={property.id} property={property} />
        ))}
      </div>
    </div>
  );
}
