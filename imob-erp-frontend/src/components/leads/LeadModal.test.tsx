import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Lead } from "@/types/lead";
import type { Role } from "@/types/common";
import { LeadModal } from "./LeadModal";

const role = vi.hoisted(() => ({ current: undefined as Role | undefined }));
vi.mock("@/hooks/useRole", () => ({ useRole: () => role.current }));

const lead = (stage: Lead["stage"]): Lead => ({
  id: "l1",
  name: "Ana Souza",
  phone: "4199",
  source: "SITE",
  stage,
  assignedTo: "u1",
  propertiesOfInterest: ["p1"],
});

describe("LeadModal - botão Criar contrato", () => {
  beforeEach(() => {
    role.current = undefined;
  });

  it("ADMIN vê o botão em lead fechado, com o link pré-preenchido", () => {
    role.current = "ADMIN";
    render(<LeadModal lead={lead("FECHADO")} onClose={() => {}} />);
    expect(screen.getByRole("link", { name: "Criar contrato" })).toHaveAttribute(
      "href",
      "/contratos/novo?leadId=l1&buyerName=Ana+Souza&propertyId=p1"
    );
  });

  it("FINANCEIRO também vê", () => {
    role.current = "FINANCEIRO";
    render(<LeadModal lead={lead("FECHADO")} onClose={() => {}} />);
    expect(screen.getByRole("link", { name: "Criar contrato" })).toBeInTheDocument();
  });

  it("CORRETOR e papel ausente não veem o botão", () => {
    for (const r of ["CORRETOR", undefined] as const) {
      role.current = r;
      const { unmount } = render(<LeadModal lead={lead("FECHADO")} onClose={() => {}} />);
      expect(screen.queryByRole("link", { name: "Criar contrato" })).not.toBeInTheDocument();
      unmount();
    }
  });

  it("ninguém vê o botão se o lead não está fechado", () => {
    role.current = "ADMIN";
    render(<LeadModal lead={lead("PROPOSTA")} onClose={() => {}} />);
    expect(screen.queryByRole("link", { name: "Criar contrato" })).not.toBeInTheDocument();
  });
});
