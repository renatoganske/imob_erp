import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/utils";
import type { Property, PropertyStatus } from "@/types/property";

const STATUS_VARIANT: Record<PropertyStatus, "success" | "warning" | "secondary" | "outline"> = {
  DISPONIVEL: "success",
  RESERVADO: "warning",
  VENDIDO: "secondary",
  ALUGADO: "secondary",
};

const STATUS_LABEL: Record<PropertyStatus, string> = {
  DISPONIVEL: "Disponível",
  RESERVADO: "Reservado",
  VENDIDO: "Vendido",
  ALUGADO: "Alugado",
};

export function PropertyCard({ property }: { property: Property }) {
  return (
    <Link
      href={`/imoveis/${property.id}`}
      className="flex flex-col gap-2 rounded-lg border border-border p-4 transition-shadow hover:shadow-md"
    >
      <div className="flex items-start justify-between">
        <h3 className="font-semibold">{property.title}</h3>
        <Badge variant={STATUS_VARIANT[property.status]}>{STATUS_LABEL[property.status]}</Badge>
      </div>
      <p className="text-sm text-muted-foreground">
        {property.neighborhood ? `${property.neighborhood}, ` : ""}
        {property.city}
      </p>
      <p className="text-lg font-bold">{formatCurrency(property.price)}</p>
      <div className="flex gap-3 text-sm text-muted-foreground">
        {property.bedrooms !== undefined && <span>{property.bedrooms} quartos</span>}
        {property.bathrooms !== undefined && <span>{property.bathrooms} banheiros</span>}
        {property.area !== undefined && <span>{property.area} m²</span>}
      </div>
    </Link>
  );
}
