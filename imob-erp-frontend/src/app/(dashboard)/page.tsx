"use client";

import Link from "next/link";
import { Building2, CalendarCheck, FileText, Users, type LucideIcon } from "lucide-react";
import { FinancialDashboard } from "@/components/financeiro/FinancialDashboard";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/state";
import { ExpiringContractsAlerts } from "@/components/contratos/ExpiringContractsAlerts";
import { useExpiringContractsSummary } from "@/hooks/useContracts";
import { useFinancialDashboard } from "@/hooks/useFinancial";
import { useRole } from "@/hooks/useRole";
import { canAccessPath, canManageContracts } from "@/lib/permissions";

const SHORTCUTS: { href: string; label: string; hint: string; icon: LucideIcon }[] = [
  { href: "/leads", label: "Novo lead", hint: "Registrar um contato", icon: Users },
  { href: "/imoveis/novo", label: "Novo imóvel", hint: "Cadastrar no portfólio", icon: Building2 },
  { href: "/visitas", label: "Visitas", hint: "Agenda e resultados", icon: CalendarCheck },
  { href: "/contratos/novo", label: "Novo contrato", hint: "Venda ou locação", icon: FileText },
];

export default function DashboardHomePage() {
  const { data, loading } = useFinancialDashboard();
  const role = useRole();
  const { data: expiring } = useExpiringContractsSummary(canManageContracts(role));

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title="Resumo" description="Visão geral do negócio e atalhos para as tarefas do dia." />

      <section aria-label="Atalhos" className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {SHORTCUTS.filter((s) => canAccessPath(role, s.href)).map(({ href, label, hint, icon: Icon }) => (
          <Link key={href} href={href} className="group">
            <Card className="flex items-center gap-3 p-4 transition-colors group-hover:border-primary/40">
              <span className="flex h-10 w-10 items-center justify-center rounded-md bg-primary-soft text-primary">
                <Icon className="h-5 w-5" />
              </span>
              <span>
                <span className="block text-sm font-medium">{label}</span>
                <span className="block text-xs text-muted-foreground">{hint}</span>
              </span>
            </Card>
          </Link>
        ))}
      </section>

      <section aria-label="Financeiro" className="flex flex-col gap-3">
        <h2 className="text-sm font-medium text-muted-foreground">Financeiro do mês</h2>
        {loading && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-24" />
            ))}
          </div>
        )}
        {data && <FinancialDashboard data={data} />}
      </section>

      {expiring && (
        <section aria-label="Contratos vencendo" className="flex flex-col gap-3">
          <h2 className="text-sm font-medium text-muted-foreground">Locações vencendo</h2>
          <ExpiringContractsAlerts summary={expiring} />
        </section>
      )}
    </div>
  );
}
