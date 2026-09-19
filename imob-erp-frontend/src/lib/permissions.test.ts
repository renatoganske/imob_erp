import { describe, expect, it } from "vitest";
import { canAccessPath, canManageContracts, parseRole } from "./permissions";

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

describe("canAccessPath", () => {
  it("bloqueia /contratos e subrotas para CORRETOR e papel ausente", () => {
    for (const path of ["/contratos", "/contratos/novo", "/contratos/abc"]) {
      expect(canAccessPath("CORRETOR", path)).toBe(false);
      expect(canAccessPath(undefined, path)).toBe(false);
      expect(canAccessPath("ADMIN", path)).toBe(true);
    }
  });
  it("não afeta outras rotas nem prefixos parecidos", () => {
    expect(canAccessPath("CORRETOR", "/leads")).toBe(true);
    expect(canAccessPath("CORRETOR", "/contratosx")).toBe(true);
    expect(canAccessPath(undefined, "/")).toBe(true);
  });
});
