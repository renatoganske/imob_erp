import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Role } from "@/types/common";
import { NavLinks } from "./NavLinks";

const role = vi.hoisted(() => ({ current: undefined as Role | undefined }));
vi.mock("@/hooks/useRole", () => ({ useRole: () => role.current }));
vi.mock("next/navigation", () => ({ usePathname: () => "/leads" }));

describe("NavLinks", () => {
  beforeEach(() => {
    role.current = undefined;
  });

  it.each<Role>(["ADMIN", "FINANCEIRO", "CORRETOR"])("mostra Contratos para %s", (r) => {
    role.current = r;
    render(<NavLinks />);
    expect(screen.getByRole("link", { name: "Contratos" })).toBeInTheDocument();
  });

  it("esconde Contratos com papel ausente, mantendo os demais itens", () => {
    role.current = undefined;
    render(<NavLinks />);
    expect(screen.queryByRole("link", { name: "Contratos" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Leads" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Financeiro" })).toBeInTheDocument();
  });
});
