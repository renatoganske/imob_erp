import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ContractDocumentUpload } from "./ContractDocumentUpload";

vi.mock("@clerk/nextjs", () => ({ useAuth: () => ({ getToken: async () => "token" }) }));

const pdf = () => new File(["%PDF"], "contrato-assinado.pdf", { type: "application/pdf" });

afterEach(() => vi.unstubAllGlobals());

describe("ContractDocumentUpload", () => {
  it("mostra o nome do arquivo escolhido antes de enviar", async () => {
    render(<ContractDocumentUpload contractId="c1" onUploaded={() => {}} />);
    await userEvent.upload(screen.getByLabelText("Arquivo PDF do contrato"), pdf());
    expect(screen.getByTestId("selected-file")).toHaveTextContent("contrato-assinado.pdf");
  });

  it("envia o arquivo e avisa o pai", async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal("fetch", fetchMock);
    const onUploaded = vi.fn();
    render(<ContractDocumentUpload contractId="c1" onUploaded={onUploaded} />);
    await userEvent.upload(screen.getByLabelText("Arquivo PDF do contrato"), pdf());
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));
    await waitFor(() => expect(onUploaded).toHaveBeenCalled());
    expect(fetchMock.mock.calls[0][0]).toContain("/api/v1/contracts/c1/document");
  });

  it("exibe o erro devolvido pela API", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, json: async () => ({ error: "Arquivo inválido" }) }));
    render(<ContractDocumentUpload contractId="c1" onUploaded={() => {}} />);
    await userEvent.upload(screen.getByLabelText("Arquivo PDF do contrato"), pdf());
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Arquivo inválido");
  });

  it("oferece substituir e link quando já há documento", () => {
    render(<ContractDocumentUpload contractId="c1" documentUrl="http://x/doc.pdf" onUploaded={() => {}} />);
    expect(screen.getByText("Substituir PDF")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Ver documento atual/ })).toHaveAttribute("href", "http://x/doc.pdf");
  });
});
