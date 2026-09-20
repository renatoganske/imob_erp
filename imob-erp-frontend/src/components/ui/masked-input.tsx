"use client";

import * as React from "react";
import { Input, type InputProps } from "@/components/ui/input";
import { centsFromDigits, formatBRL, onlyDigits, parseBRL } from "@/lib/masks";

type BaseProps = Omit<InputProps, "value" | "onChange" | "type">;

// Campo de dígitos com máscara (CPF/CNPJ, telefone). `value` e `onValueChange` trafegam só dígitos;
// a máscara é apresentação. `validate` devolve a mensagem de erro, exibida após sair do campo.
export function MaskedInput({
  mask,
  value,
  onValueChange,
  validate,
  id,
  onBlur,
  ...props
}: BaseProps & {
  mask: (text: string) => string;
  value: string;
  onValueChange: (digits: string) => void;
  validate?: (digits: string) => string | null;
}) {
  const [touched, setTouched] = React.useState(false);
  const error = touched && validate ? validate(onlyDigits(value)) : null;
  const errorId = id ? `${id}-error` : undefined;

  return (
    <>
      <Input
        {...props}
        id={id}
        type="text"
        inputMode="numeric"
        value={mask(value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        onChange={(e) => onValueChange(onlyDigits(mask(e.target.value)))}
        onBlur={(e) => {
          setTouched(true);
          onBlur?.(e);
        }}
      />
      {error && (
        <p id={errorId} role="alert" className="mt-1 text-xs text-danger">
          {error}
        </p>
      )}
    </>
  );
}

// Campo de valor em reais: exibe "R$ 1.234,56" enquanto digita e entrega número puro (0 = vazio).
export function CurrencyInput({
  value,
  onValueChange,
  ...props
}: BaseProps & { value: number; onValueChange: (value: number) => void }) {
  return (
    <Input
      {...props}
      type="text"
      inputMode="numeric"
      value={value > 0 ? formatBRL(value) : ""}
      onChange={(e) => onValueChange(centsFromDigits(e.target.value))}
      onPaste={(e) => {
        const pasted = parseBRL(e.clipboardData.getData("text"));
        if (pasted === null) return;
        e.preventDefault();
        onValueChange(Math.round(pasted * 100) / 100);
      }}
    />
  );
}
