import { ContractForm } from "@/components/contratos/ContractForm";

export default function NewContractPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Novo contrato</h1>
      <ContractForm />
    </div>
  );
}
