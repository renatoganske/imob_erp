import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { VisitFilters } from "./VisitFilters";

describe("VisitFilters", () => {
  it("emite o status selecionado e limpa ao voltar para todos", async () => {
    const onChange = vi.fn();
    render(<VisitFilters filters={{}} onChange={onChange} />);
    await userEvent.selectOptions(screen.getByLabelText("Status"), "REALIZADA");
    expect(onChange).toHaveBeenLastCalledWith({ status: "REALIZADA" });
  });

  it("emite o período e limpa ao esvaziar", async () => {
    const onChange = vi.fn();
    render(<VisitFilters filters={{ from: "2026-10-01" }} onChange={onChange} />);
    await userEvent.type(screen.getByLabelText("Até"), "2026-10-31");
    expect(onChange).toHaveBeenLastCalledWith({ from: "2026-10-01", to: "2026-10-31" });
    await userEvent.clear(screen.getByLabelText("De"));
    expect(onChange).toHaveBeenLastCalledWith({ from: undefined });
  });
});
