import { ContractForm } from "@/components/contratos/ContractForm";
import { PageHeader } from "@/components/ui/page-header";

export default function NewContractPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Novo contrato" description="Registre uma venda ou locação. O contrato começa como rascunho até ser ativado." />
      <ContractForm />
    </div>
  );
}
