export type PropertyType = "CASA" | "APARTAMENTO" | "COMERCIAL" | "TERRENO";
export type PropertyStatus = "DISPONIVEL" | "RESERVADO" | "VENDIDO" | "ALUGADO";
export type PropertyPurpose = "VENDA" | "ALUGUEL" | "AMBOS";

export interface Property {
  id: string;
  type: PropertyType;
  title: string;
  description?: string;
  address: string;
  neighborhood?: string;
  city: string;
  price: number;
  area?: number;
  bedrooms?: number;
  bathrooms?: number;
  parkingSpots?: number;
  status: PropertyStatus;
  purpose: PropertyPurpose;
  photos: string[];
}

export type PropertyRequest = Omit<Property, "id" | "status" | "photos">;

export interface PropertyFilters {
  type?: PropertyType;
  status?: PropertyStatus;
  neighborhood?: string;
  minPrice?: number;
  maxPrice?: number;
}
