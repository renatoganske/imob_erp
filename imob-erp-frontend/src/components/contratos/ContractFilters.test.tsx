import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ContractFilters } from "./ContractFilters";

describe("ContractFilters", () => {
  it("emite status e tipo selecionados", async () => {
    const onChange = vi.fn();
    render(<ContractFilters filters={{}} onChange={onChange} />);
    await userEvent.selectOptions(screen.getByLabelText("Status"), "ATIVO");
    expect(onChange).toHaveBeenLastCalledWith({ status: "ATIVO" });
    await userEvent.selectOptions(screen.getByLabelText("Tipo"), "LOCACAO");
    expect(onChange).toHaveBeenLastCalledWith({ type: "LOCACAO" });
  });

  it("emite o período e limpa ao esvaziar", async () => {
    const onChange = vi.fn();
    render(<ContractFilters filters={{ from: "2026-01-01" }} onChange={onChange} />);
    await userEvent.type(screen.getByLabelText("Início até"), "2026-12-31");
    expect(onChange).toHaveBeenLastCalledWith({ from: "2026-01-01", to: "2026-12-31" });
    await userEvent.clear(screen.getByLabelText("Início de"));
    expect(onChange).toHaveBeenLastCalledWith({ from: undefined });
  });
});
