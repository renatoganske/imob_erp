// Máscaras e validadores de apresentação. O backend recebe/armazena só dígitos e número puro.

export const onlyDigits = (text: string): string => text.replace(/\D/g, "");

const BRL = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

// "R$ 1.234,56" (espaço comum, não o NBSP do Intl, para casar com o que o usuário digita/cola).
export const formatBRL = (value: number): string => BRL.format(value).replace(/ /g, " ");

// Texto → número. Aceita "R$ 1.234,56", "1234,56" e "1234.56"; sem número válido devolve null.
export function parseBRL(text: string): number | null {
  const cleaned = text.replace(/[^\d.,]/g, "");
  if (!/\d/.test(cleaned)) return null;
  const normalized = cleaned.includes(",")
    ? cleaned.replace(/\./g, "").replace(",", ".")
    : /^\d+\.\d{1,2}$/.test(cleaned)
      ? cleaned
      : cleaned.replace(/\./g, "");
  const n = Number(normalized);
  return Number.isFinite(n) ? n : null;
}

// Digitação estilo "centavos": cada dígito entra à direita ("1","12","123" → 0,01 / 0,12 / 1,23).
export const centsFromDigits = (text: string): number => Number(onlyDigits(text) || "0") / 100;

const applyPattern = (digits: string, pattern: string): string =>
  [...pattern].reduce(
    (acc, ch) => {
      if (acc.rest === "") return acc;
      return ch === "#" ? { out: acc.out + acc.rest[0], rest: acc.rest.slice(1) } : { out: acc.out + ch, rest: acc.rest };
    },
    { out: "", rest: digits }
  ).out;

// Até 11 dígitos → CPF; acima disso → CNPJ (máx. 14).
export function formatCpfCnpj(text: string): string {
  const digits = onlyDigits(text).slice(0, 14);
  return applyPattern(digits, digits.length <= 11 ? "###.###.###-##" : "##.###.###/####-##");
}

// Fixo (10 dígitos) "(41) 3333-1234" e celular (11) "(41) 99999-1234".
export function formatPhone(text: string): string {
  const digits = onlyDigits(text).slice(0, 11);
  return applyPattern(digits, digits.length <= 10 ? "(##) ####-####" : "(##) #####-####");
}

const checkDigit = (digits: number[], weights: number[]): number => {
  const rest = digits.reduce((sum, d, i) => sum + d * weights[i], 0) % 11;
  return rest < 2 ? 0 : 11 - rest;
};

const hasValidCheckDigits = (digits: string, weights1: number[], weights2: number[]): boolean => {
  if (/^(\d)\1+$/.test(digits)) return false;
  const nums = [...digits].map(Number);
  const size = weights1.length;
  return checkDigit(nums.slice(0, size), weights1) === nums[size] && checkDigit(nums.slice(0, size + 1), weights2) === nums[size + 1];
};

export const validateCpf = (text: string): boolean => {
  const digits = onlyDigits(text);
  return digits.length === 11 && hasValidCheckDigits(digits, [10, 9, 8, 7, 6, 5, 4, 3, 2], [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);
};

export const validateCnpj = (text: string): boolean => {
  const digits = onlyDigits(text);
  return digits.length === 14 && hasValidCheckDigits(digits, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2], [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
};

// Mensagem de erro para CPF/CNPJ preenchido; null se vazio ou válido.
export function documentError(text: string): string | null {
  const digits = onlyDigits(text);
  if (digits === "") return null;
  return (digits.length <= 11 ? validateCpf(digits) : validateCnpj(digits)) ? null : "CPF/CNPJ inválido";
}
