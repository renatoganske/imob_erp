import Link from "next/link";
import { Bath, BedDouble, MapPin, Ruler } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { PROPERTY_STATUS_LABEL } from "@/lib/labels";
import { formatCurrency } from "@/lib/utils";
import type { Property, PropertyStatus } from "@/types/property";

const STATUS_VARIANT: Record<PropertyStatus, "success" | "warning" | "secondary" | "outline"> = {
  DISPONIVEL: "success",
  RESERVADO: "warning",
  VENDIDO: "secondary",
  ALUGADO: "secondary",
};


export function PropertyCard({ property }: { property: Property }) {
  return (
    <Link
      href={`/imoveis/${property.id}`}
      className="flex flex-col gap-3 rounded-lg border border-border bg-card p-4 shadow-card transition-colors hover:border-primary/40"
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-medium leading-snug">{property.title}</h3>
        <Badge variant={STATUS_VARIANT[property.status]}>{PROPERTY_STATUS_LABEL[property.status]}</Badge>
      </div>
      <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
        <MapPin className="h-3.5 w-3.5 shrink-0" />
        {property.neighborhood ? `${property.neighborhood}, ` : ""}
        {property.city}
      </p>
      <p className="text-xl font-semibold tabular-nums tracking-tight">{formatCurrency(property.price)}</p>
      <div className="flex gap-4 border-t border-border pt-3 text-sm text-muted-foreground">
        {property.bedrooms !== undefined && (
          <span className="flex items-center gap-1.5" title="Quartos">
            <BedDouble className="h-4 w-4" />
            {property.bedrooms}
          </span>
        )}
        {property.bathrooms !== undefined && (
          <span className="flex items-center gap-1.5" title="Banheiros">
            <Bath className="h-4 w-4" />
            {property.bathrooms}
          </span>
        )}
        {property.area !== undefined && (
          <span className="flex items-center gap-1.5" title="Área">
            <Ruler className="h-4 w-4" />
            {property.area} m²
          </span>
        )}
      </div>
    </Link>
  );
}
