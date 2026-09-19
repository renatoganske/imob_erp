import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { Contract } from "@/types/contract";
import { ActivateContractDialog } from "./ActivateContractDialog";

const base: Contract = {
  id: "c1",
  propertyId: "p",
  agentId: "a",
  type: "LOCACAO",
  status: "RASCUNHO",
  value: 2500,
  startDate: "2026-10-01",
  buyerName: "Ana",
  buyerDocument: "1",
  ownerName: "Bia",
  ownerDocument: "2",
};

function setup(contract: Contract) {
  const onConfirm = vi.fn();
  const onCancel = vi.fn();
  render(<ActivateContractDialog contract={contract} open onConfirm={onConfirm} onCancel={onCancel} />);
  return { onConfirm, onCancel };
}

describe("ActivateContractDialog", () => {
  it("resume valor e 12 parcelas na locação", () => {
    setup(base);
    expect(screen.getByText(/2\.500,00/)).toBeInTheDocument();
    expect(screen.getByText("12 parcelas mensais")).toBeInTheDocument();
    expect(screen.getByText(/passa a Alugado/)).toBeInTheDocument();
  });

  it("resume 1 lançamento na compra e venda", () => {
    setup({ ...base, type: "COMPRA_VENDA", value: 400000 });
    expect(screen.getByText("1 lançamento")).toBeInTheDocument();
    expect(screen.getByText(/passa a Vendido/)).toBeInTheDocument();
  });

  it("não exibe valor de comissão", () => {
    setup(base);
    expect(screen.queryByText(/R\$.*comiss/i)).not.toBeInTheDocument();
    expect(screen.getByText(/comissão do corretor é calculada/i)).toBeInTheDocument();
  });

  it("confirma e cancela pelos botões", async () => {
    const { onConfirm, onCancel } = setup(base);
    await userEvent.click(screen.getByRole("button", { name: "Ativar contrato" }));
    expect(onConfirm).toHaveBeenCalledOnce();
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(onCancel).toHaveBeenCalledOnce();
  });
});
