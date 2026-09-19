import { describe, expect, it } from "vitest";
import { canAccessPath, canManageContracts, canReadContracts, parseRole } from "./permissions";

describe("parseRole", () => {
  it("aceita os papéis conhecidos e descarta o resto", () => {
    expect(parseRole("ADMIN")).toBe("ADMIN");
    expect(parseRole("CORRETOR")).toBe("CORRETOR");
    expect(parseRole("FINANCEIRO")).toBe("FINANCEIRO");
    expect(parseRole("admin")).toBeUndefined();
    expect(parseRole(undefined)).toBeUndefined();
    expect(parseRole(42)).toBeUndefined();
  });
});

describe("canManageContracts", () => {
  it("libera ADMIN e FINANCEIRO; nega CORRETOR e papel ausente", () => {
    expect(canManageContracts("ADMIN")).toBe(true);
    expect(canManageContracts("FINANCEIRO")).toBe(true);
    expect(canManageContracts("CORRETOR")).toBe(false);
    expect(canManageContracts(undefined)).toBe(false);
  });
});

describe("canReadContracts", () => {
  it("libera os três papéis; nega papel ausente", () => {
    expect(canReadContracts("ADMIN")).toBe(true);
    expect(canReadContracts("FINANCEIRO")).toBe(true);
    expect(canReadContracts("CORRETOR")).toBe(true);
    expect(canReadContracts(undefined)).toBe(false);
  });
});

describe("canAccessPath", () => {
  it("CORRETOR lê a lista e o detalhe, mas não acessa a criação", () => {
    expect(canAccessPath("CORRETOR", "/contratos")).toBe(true);
    expect(canAccessPath("CORRETOR", "/contratos/abc")).toBe(true);
    expect(canAccessPath("CORRETOR", "/contratos/novo")).toBe(false);
    expect(canAccessPath("CORRETOR", "/contratos/novo/qualquer")).toBe(false);
  });
  it("papel ausente não acessa nenhuma rota de contratos", () => {
    for (const path of ["/contratos", "/contratos/novo", "/contratos/abc"]) {
      expect(canAccessPath(undefined, path)).toBe(false);
    }
  });
  it("ADMIN e FINANCEIRO acessam todas as rotas de contratos", () => {
    for (const role of ["ADMIN", "FINANCEIRO"] as const) {
      for (const path of ["/contratos", "/contratos/novo", "/contratos/abc"]) {
        expect(canAccessPath(role, path)).toBe(true);
      }
    }
  });
  it("não afeta outras rotas nem prefixos parecidos", () => {
    expect(canAccessPath("CORRETOR", "/leads")).toBe(true);
    expect(canAccessPath("CORRETOR", "/contratosx")).toBe(true);
    expect(canAccessPath(undefined, "/")).toBe(true);
  });
});
