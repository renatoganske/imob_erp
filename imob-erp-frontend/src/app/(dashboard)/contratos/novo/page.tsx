import { ContractForm } from "@/components/contratos/ContractForm";
import { PageHeader } from "@/components/ui/page-header";
import { parsePrefill } from "@/lib/contracts";

export default async function NewContractPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const prefill = parsePrefill(await searchParams);
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Novo contrato" description="Registre uma venda ou locação. O contrato começa como rascunho até ser ativado." />
      <ContractForm prefill={prefill} />
    </div>
  );
}
