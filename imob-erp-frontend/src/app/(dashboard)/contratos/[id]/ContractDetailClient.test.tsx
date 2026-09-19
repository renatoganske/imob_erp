import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Contract } from "@/types/contract";
import type { Role } from "@/types/common";
import { ContractDetailClient } from "./ContractDetailClient";

const role = vi.hoisted(() => ({ current: undefined as Role | undefined }));
const contract = vi.hoisted(() => ({ current: undefined as unknown }));

vi.mock("@/hooks/useRole", () => ({ useRole: () => role.current }));
vi.mock("@clerk/nextjs", () => ({ useAuth: () => ({ getToken: async () => "t" }) }));
vi.mock("@/hooks/useContracts", () => ({ useContractMutations: () => ({ updateStatus: vi.fn() }) }));
vi.mock("@/lib/api", () => ({
  ApiRequestError: class extends Error {},
  api: { get: async () => contract.current },
}));

const base: Contract = {
  id: "c1",
  propertyId: "p1",
  agentId: "u1",
  type: "COMPRA_VENDA",
  status: "RASCUNHO",
  value: 500000,
  startDate: "2026-01-01",
  buyerName: "Comprador Fulano",
  buyerDocument: "111.111.111-11",
  ownerName: "Dono Beltrano",
  ownerDocument: "222.222.222-22",
  documentUrl: "http://r2/doc.pdf",
};

describe("ContractDetailClient", () => {
  beforeEach(() => {
    role.current = undefined;
  });

  it.each<Role>(["ADMIN", "FINANCEIRO"])("%s vê ativação, documento e CPF/CNPJ", async (r) => {
    role.current = r;
    contract.current = base;
    render(<ContractDetailClient id="c1" />);
    expect(await screen.findByRole("button", { name: "Ativar contrato" })).toBeInTheDocument();
    expect(screen.getByLabelText("Documento do contrato")).toBeInTheDocument();
    expect(screen.getByText(/111\.111\.111-11/)).toBeInTheDocument();
  });

  it("CORRETOR vê o contrato em modo somente leitura, sem ativar, sem PDF e sem CPF/CNPJ", async () => {
    role.current = "CORRETOR";
    // o backend já omite CPF/CNPJ e a URL do PDF para o corretor
    contract.current = { ...base, buyerDocument: undefined, ownerDocument: undefined, documentUrl: undefined };
    render(<ContractDetailClient id="c1" />);
    await waitFor(() => expect(screen.getByText("Comprador Fulano")).toBeInTheDocument());
    expect(screen.getByText("Dono Beltrano")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Ativar contrato" })).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Documento do contrato")).not.toBeInTheDocument();
    expect(screen.queryByText(/—/)).not.toBeInTheDocument();
    expect(screen.getByText(/Somente leitura/)).toBeInTheDocument();
  });
});
