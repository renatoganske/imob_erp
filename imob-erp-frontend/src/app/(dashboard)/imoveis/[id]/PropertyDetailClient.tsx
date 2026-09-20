"use client";

import { useAuth } from "@clerk/nextjs";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Bath, BedDouble, Car, MapPin, Pencil, Ruler, type LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { PhotoUpload } from "@/components/imoveis/PhotoUpload";
import { useRole } from "@/hooks/useRole";
import { canManagePhotos } from "@/lib/permissions";
import { Skeleton } from "@/components/ui/state";
import { api } from "@/lib/api";
import { PROPERTY_PURPOSE_LABEL, PROPERTY_STATUS_LABEL, PROPERTY_TYPE_LABEL } from "@/lib/labels";
import { formatCurrency } from "@/lib/utils";
import type { Property, PropertyStatus } from "@/types/property";

const STATUS_VARIANT: Record<PropertyStatus, "success" | "warning" | "secondary"> = {
  DISPONIVEL: "success",
  RESERVADO: "warning",
  VENDIDO: "secondary",
  ALUGADO: "secondary",
};

function Feature({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value?: number | string }) {
  if (value === undefined || value === null || value === "") return null;
  return (
    <div className="flex items-center gap-3">
      <span className="flex h-9 w-9 items-center justify-center rounded-md bg-primary-soft text-primary">
        <Icon className="h-4 w-4" />
      </span>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-sm font-medium tabular-nums">{value}</p>
      </div>
    </div>
  );
}

export function PropertyDetailClient({ id }: { id: string }) {
  const { getToken } = useAuth();
  const role = useRole();
  const [property, setProperty] = useState<Property | null>(null);
  const [error, setError] = useState(false);

  function load() {
    api
      .get<Property>(`/api/v1/properties/${id}`, { getToken })
      .then(setProperty)
      .catch(() => setError(true));
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (error) {
    return (
      <p role="alert" className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
        Não foi possível carregar o imóvel.
      </p>
    );
  }

  if (!property) {
    return (
      <div role="status" aria-label="Carregando" className="flex flex-col gap-4">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-5 w-1/2" />
        <Skeleton className="h-28 w-full" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-xl font-semibold tracking-tight sm:text-2xl">{property.title}</h1>
          <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground">
            <MapPin className="h-3.5 w-3.5 shrink-0" />
            {property.address}
            {property.neighborhood ? ` — ${property.neighborhood}` : ""}, {property.city}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant={STATUS_VARIANT[property.status]}>{PROPERTY_STATUS_LABEL[property.status]}</Badge>
          <Button asChild variant="outline">
            <Link href={`/imoveis/${property.id}/editar`}>
              <Pencil className="h-4 w-4" />
              Editar
            </Link>
          </Button>
        </div>
      </div>

      <Card className="flex flex-col gap-5 p-5">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <p className="text-2xl font-semibold tabular-nums tracking-tight">{formatCurrency(property.price)}</p>
          <p className="text-sm text-muted-foreground">
            {PROPERTY_TYPE_LABEL[property.type]} · {PROPERTY_PURPOSE_LABEL[property.purpose]}
          </p>
        </div>
        <div className="grid grid-cols-2 gap-4 border-t border-border pt-5 sm:grid-cols-4">
          <Feature icon={Ruler} label="Área" value={property.area !== undefined ? `${property.area} m²` : undefined} />
          <Feature icon={BedDouble} label="Quartos" value={property.bedrooms} />
          <Feature icon={Bath} label="Banheiros" value={property.bathrooms} />
          <Feature icon={Car} label="Vagas" value={property.parkingSpots} />
        </div>
      </Card>

      {property.description && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium text-muted-foreground">Descrição</h2>
          <p className="whitespace-pre-line text-sm leading-relaxed">{property.description}</p>
        </section>
      )}

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-medium text-muted-foreground">Fotos</h2>
        <PhotoUpload
          propertyId={property.id}
          photos={property.photos}
          onUploaded={load}
          canDelete={canManagePhotos(role)}
          canReorder={canManagePhotos(role)}
        />
      </section>
    </div>
  );
}
