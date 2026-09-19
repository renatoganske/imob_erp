"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { ApiRequestError } from "@/lib/api";
import { usePropertyMutations } from "@/hooks/useProperties";
import type { Property, PropertyRequest } from "@/types/property";

const EMPTY: PropertyRequest = {
  type: "APARTAMENTO",
  title: "",
  description: "",
  address: "",
  neighborhood: "",
  city: "",
  price: 0,
  area: undefined,
  bedrooms: undefined,
  bathrooms: undefined,
  parkingSpots: undefined,
  purpose: "VENDA",
};

export function PropertyForm({ property }: { property?: Property }) {
  const router = useRouter();
  const { create, update } = usePropertyMutations();
  const [form, setForm] = useState<PropertyRequest>(property ?? EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const saved = property ? await update(property.id, form) : await create(form);
      router.push(`/imoveis/${saved.id}`);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Não foi possível salvar o imóvel");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex max-w-xl flex-col gap-4">
      <div>
        <Label htmlFor="title">Título</Label>
        <Input id="title" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="type">Tipo</Label>
          <Select
            id="type"
            className="w-full"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as PropertyRequest["type"] })}
          >
            <option value="CASA">Casa</option>
            <option value="APARTAMENTO">Apartamento</option>
            <option value="COMERCIAL">Comercial</option>
            <option value="TERRENO">Terreno</option>
          </Select>
        </div>
        <div>
          <Label htmlFor="purpose">Finalidade</Label>
          <Select
            id="purpose"
            className="w-full"
            value={form.purpose}
            onChange={(e) => setForm({ ...form, purpose: e.target.value as PropertyRequest["purpose"] })}
          >
            <option value="VENDA">Venda</option>
            <option value="ALUGUEL">Aluguel</option>
            <option value="AMBOS">Ambos</option>
          </Select>
        </div>
      </div>

      <div>
        <Label htmlFor="address">Endereço</Label>
        <Input id="address" required value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="neighborhood">Bairro</Label>
          <Input id="neighborhood" value={form.neighborhood ?? ""} onChange={(e) => setForm({ ...form, neighborhood: e.target.value })} />
        </div>
        <div>
          <Label htmlFor="city">Cidade</Label>
          <Input id="city" required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
        </div>
      </div>

      <div>
        <Label htmlFor="price">Preço</Label>
        <Input
          id="price"
          type="number"
          required
          value={form.price}
          onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
        />
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div>
          <Label htmlFor="area">Área (m²)</Label>
          <Input id="area" type="number" value={form.area ?? ""} onChange={(e) => setForm({ ...form, area: Number(e.target.value) })} />
        </div>
        <div>
          <Label htmlFor="bedrooms">Quartos</Label>
          <Input id="bedrooms" type="number" value={form.bedrooms ?? ""} onChange={(e) => setForm({ ...form, bedrooms: Number(e.target.value) })} />
        </div>
        <div>
          <Label htmlFor="bathrooms">Banheiros</Label>
          <Input id="bathrooms" type="number" value={form.bathrooms ?? ""} onChange={(e) => setForm({ ...form, bathrooms: Number(e.target.value) })} />
        </div>
        <div>
          <Label htmlFor="parkingSpots">Vagas</Label>
          <Input id="parkingSpots" type="number" value={form.parkingSpots ?? ""} onChange={(e) => setForm({ ...form, parkingSpots: Number(e.target.value) })} />
        </div>
      </div>

      <div>
        <Label htmlFor="description">Descrição</Label>
        <textarea
          id="description"
          className="min-h-24 w-full rounded-md border border-border bg-card px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
          value={form.description ?? ""}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
      </div>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <Button type="submit" disabled={submitting}>
        {submitting ? "Salvando..." : "Salvar"}
      </Button>
    </form>
  );
}
