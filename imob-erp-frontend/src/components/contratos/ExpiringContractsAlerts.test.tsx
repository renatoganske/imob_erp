import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Contract } from "@/types/contract";
import { ContractList } from "./ContractList";
import { ExpiringContractsAlerts } from "./ExpiringContractsAlerts";
import { ExpiryBadge } from "./ExpiryBadge";

describe("ExpiringContractsAlerts", () => {
  it("mostra a contagem de cada janela com link para a lista filtrada", () => {
    render(<ExpiringContractsAlerts summary={{ within30Days: 2, within60Days: 5, within90Days: 9 }} />);

    const links = screen.getAllByRole("link");
    expect(links.map((l) => l.getAttribute("href"))).toEqual([
      "/contratos?expiringInDays=30",
      "/contratos?expiringInDays=60",
      "/contratos?expiringInDays=90",
    ]);
    expect(within(links[0]).getByText("Vencem em até 30 dias")).toBeInTheDocument();
    expect(within(links[0]).getByText("2")).toBeInTheDocument();
    expect(within(links[1]).getByText("5")).toBeInTheDocument();
    expect(within(links[2]).getByText("9")).toBeInTheDocument();
    expect(screen.queryByText(/Nenhuma locação ativa/)).not.toBeInTheDocument();
  });

  it("marca a janela ativa", () => {
    render(<ExpiringContractsAlerts summary={{ within30Days: 1, within60Days: 1, within90Days: 1 }} activeWindow={60} />);

    const links = screen.getAllByRole("link");
    expect(links[1]).toHaveAttribute("aria-current", "true");
    expect(links[0]).not.toHaveAttribute("aria-current");
  });

  it("avisa quando nada vence em 90 dias", () => {
    render(<ExpiringContractsAlerts summary={{ within30Days: 0, within60Days: 0, within90Days: 0 }} />);

    expect(screen.getByText("Nenhuma locação ativa vence nos próximos 90 dias.")).toBeInTheDocument();
  });
});

describe("ExpiryBadge", () => {
  const base = { status: "ATIVO", type: "LOCACAO" } as const;

  it("mostra quantos dias faltam para locação ativa dentro de 90 dias", () => {
    render(<ExpiryBadge contract={{ ...base, endDate: "2026-10-02" }} today="2026-09-20" />);
    expect(screen.getByText("Vence em 12 dias")).toBeInTheDocument();
  });

  it("não renderiza fora da janela, sem data de fim ou para outro tipo/status", () => {
    const { container, rerender } = render(<ExpiryBadge contract={{ ...base, endDate: "2027-01-01" }} today="2026-09-20" />);
    expect(container).toBeEmptyDOMElement();
    rerender(<ExpiryBadge contract={{ ...base, endDate: undefined }} today="2026-09-20" />);
    expect(container).toBeEmptyDOMElement();
    rerender(<ExpiryBadge contract={{ status: "RASCUNHO", type: "LOCACAO", endDate: "2026-10-02" }} today="2026-09-20" />);
    expect(container).toBeEmptyDOMElement();
    rerender(<ExpiryBadge contract={{ status: "ATIVO", type: "COMPRA_VENDA", endDate: "2026-10-02" }} today="2026-09-20" />);
    expect(container).toBeEmptyDOMElement();
  });
});

describe("ContractList", () => {
  const contract = (id: string, endDate?: string): Contract => ({
    id,
    propertyId: "p",
    agentId: "a",
    type: "LOCACAO",
    status: "ATIVO",
    value: 2000,
    startDate: "2026-01-01",
    endDate,
    buyerName: "b",
    ownerName: "o",
  });

  it("exibe o selo de vencimento só nos contratos que vencem em até 90 dias", () => {
    render(
      <ContractList
        today="2026-09-20"
        contracts={[contract("a", "2026-09-25"), contract("b", "2027-06-01"), contract("c")]}
      />,
    );

    expect(screen.getAllByText(/^Vence /)).toHaveLength(1);
    expect(screen.getByText("Vence em 5 dias")).toBeInTheDocument();
  });
});
