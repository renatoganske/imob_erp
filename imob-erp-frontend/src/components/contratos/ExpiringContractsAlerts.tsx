import Link from "next/link";
import { Card } from "@/components/ui/card";
import {
  expiringContractsHref,
  expiryAlerts,
  type ExpiryLevel,
  type ExpiryWindow,
} from "@/lib/contracts";
import { cn } from "@/lib/utils";
import type { ExpiringSummary } from "@/types/contract";

const LEVEL_STYLE: Record<ExpiryLevel, { count: string; border: string }> = {
  danger: { count: "text-danger", border: "hover:border-danger/40" },
  warning: { count: "text-warning", border: "hover:border-warning/40" },
  info: { count: "text-info", border: "hover:border-info/40" },
};

// Locações ativas vencendo em 30/60/90 dias (contagem acumulada); cada cartão abre a lista filtrada.
export function ExpiringContractsAlerts({
  summary,
  activeWindow,
}: {
  summary: ExpiringSummary;
  activeWindow?: ExpiryWindow;
}) {
  const alerts = expiryAlerts(summary);
  const total = alerts[alerts.length - 1].count;

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {alerts.map(({ window, count, level }) => (
          <Link
            key={window}
            href={expiringContractsHref(window)}
            aria-current={window === activeWindow ? "true" : undefined}
            className="group"
          >
            <Card
              className={cn(
                "flex items-baseline justify-between gap-3 p-4 transition-colors",
                LEVEL_STYLE[level].border,
                window === activeWindow && "border-primary",
              )}
            >
              <span className="text-sm text-muted-foreground">Vencem em até {window} dias</span>
              <span className={cn("text-2xl font-semibold tabular-nums", count > 0 && LEVEL_STYLE[level].count)}>
                {count}
              </span>
            </Card>
          </Link>
        ))}
      </div>
      {total === 0 && (
        <p className="text-sm text-muted-foreground">Nenhuma locação ativa vence nos próximos 90 dias.</p>
      )}
    </div>
  );
}
