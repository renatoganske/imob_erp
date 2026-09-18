"use client";

import { useAuth } from "@clerk/nextjs";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PhotoUpload } from "@/components/imoveis/PhotoUpload";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import type { Property } from "@/types/property";

export function PropertyDetailClient({ id }: { id: string }) {
  const { getToken } = useAuth();
  const [property, setProperty] = useState<Property | null>(null);

  function load() {
    api.get<Property>(`/api/v1/properties/${id}`, { getToken }).then(setProperty);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (!property) return <p className="text-muted-foreground">Carregando...</p>;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{property.title}</h1>
          <p className="text-muted-foreground">
            {property.address} — {property.neighborhood}, {property.city}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Badge>{property.status}</Badge>
          <Button asChild variant="outline">
            <Link href={`/imoveis/${property.id}/editar`}>Editar</Link>
          </Button>
        </div>
      </div>

      <p className="text-xl font-bold">{formatCurrency(property.price)}</p>

      {property.description && <p>{property.description}</p>}

      <PhotoUpload propertyId={property.id} photos={property.photos} onUploaded={load} />
    </div>
  );
}
