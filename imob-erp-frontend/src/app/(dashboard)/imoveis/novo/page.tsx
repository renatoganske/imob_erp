import { PropertyForm } from "@/components/imoveis/PropertyForm";

export default function NewPropertyPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Novo imóvel</h1>
      <PropertyForm />
    </div>
  );
}
