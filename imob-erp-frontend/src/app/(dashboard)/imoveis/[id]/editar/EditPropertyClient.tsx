"use client";

import { useAuth } from "@clerk/nextjs";
import { useEffect, useState } from "react";
import { PropertyForm } from "@/components/imoveis/PropertyForm";
import { api } from "@/lib/api";
import type { Property } from "@/types/property";

export function EditPropertyClient({ id }: { id: string }) {
  const { getToken } = useAuth();
  const [property, setProperty] = useState<Property | null>(null);

  useEffect(() => {
    api.get<Property>(`/api/v1/properties/${id}`, { getToken }).then(setProperty);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (!property) return <p className="text-muted-foreground">Carregando...</p>;

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Editar imóvel</h1>
      <PropertyForm property={property} />
    </div>
  );
}
