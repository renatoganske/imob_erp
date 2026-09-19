import { PropertyForm } from "@/components/imoveis/PropertyForm";
import { PageHeader } from "@/components/ui/page-header";

export default function NewPropertyPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Novo imóvel" description="Cadastre um imóvel no portfólio." />
      <PropertyForm />
    </div>
  );
}
