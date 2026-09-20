import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { documentError, formatCpfCnpj } from "@/lib/masks";
import { CurrencyInput, MaskedInput } from "./masked-input";

function Currency({ onChange }: { onChange: (v: number) => void }) {
  const [v, setV] = useState(0);
  return (
    <>
      <label htmlFor="v">Valor</label>
      <CurrencyInput
        id="v"
        value={v}
        onValueChange={(n) => {
          setV(n);
          onChange(n);
        }}
      />
    </>
  );
}

function Doc({ onChange, initial = "" }: { onChange: (v: string) => void; initial?: string }) {
  const [v, setV] = useState(initial);
  return (
    <>
      <label htmlFor="d">CPF/CNPJ</label>
      <MaskedInput
        id="d"
        mask={formatCpfCnpj}
        validate={documentError}
        value={v}
        onValueChange={(d) => {
          setV(d);
          onChange(d);
        }}
      />
    </>
  );
}

describe("CurrencyInput", () => {
  it("formata enquanto digita e envia número puro", () => {
    const onChange = vi.fn();
    render(<Currency onChange={onChange} />);
    const input = screen.getByLabelText("Valor") as HTMLInputElement;
    expect(input).toHaveAttribute("inputmode", "numeric");
    fireEvent.change(input, { target: { value: "1" } });
    fireEvent.change(input, { target: { value: "123456" } });
    expect(input.value).toBe("R$ 1.234,56");
    expect(onChange).toHaveBeenLastCalledWith(1234.56);
  });

  it("colar texto formatado funciona", () => {
    const onChange = vi.fn();
    render(<Currency onChange={onChange} />);
    const input = screen.getByLabelText("Valor") as HTMLInputElement;
    fireEvent.paste(input, { clipboardData: { getData: () => "R$ 2.500,00" } });
    expect(onChange).toHaveBeenLastCalledWith(2500);
    expect(input.value).toBe("R$ 2.500,00");
  });
});

describe("MaskedInput", () => {
  it("exibe máscara e entrega só dígitos", () => {
    const onChange = vi.fn();
    render(<Doc onChange={onChange} />);
    const input = screen.getByLabelText("CPF/CNPJ") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "52998224725" } });
    expect(input.value).toBe("529.982.247-25");
    expect(onChange).toHaveBeenLastCalledWith("52998224725");
  });

  it("valor antigo já salvo com máscara é exibido formatado", () => {
    render(<Doc onChange={vi.fn()} initial="52998224725" />);
    expect((screen.getByLabelText("CPF/CNPJ") as HTMLInputElement).value).toBe("529.982.247-25");
  });

  it("mostra erro inline ao sair do campo com dígito verificador inválido", () => {
    render(<Doc onChange={vi.fn()} />);
    const input = screen.getByLabelText("CPF/CNPJ");
    fireEvent.change(input, { target: { value: "52998224726" } });
    expect(screen.queryByRole("alert")).toBeNull();
    fireEvent.blur(input);
    expect(screen.getByRole("alert")).toHaveTextContent("CPF/CNPJ inválido");
    expect(input).toHaveAttribute("aria-invalid", "true");
  });
});
