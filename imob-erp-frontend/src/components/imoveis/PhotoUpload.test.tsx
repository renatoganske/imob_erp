import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MAX_PHOTO_BYTES, MAX_PHOTOS, PhotoUpload, UPLOAD_CONCURRENCY, photoKey, planUpload } from "./PhotoUpload";

vi.mock("@clerk/nextjs", () => ({ useAuth: () => ({ getToken: async () => "t" }) }));

const png = (name: string) => new File(["x"], name, { type: "image/png" });
const ok = () => Promise.resolve(new Response("{}", { status: 200 }));

function setup(photos: string[] = []) {
  const onUploaded = vi.fn();
  render(<PhotoUpload propertyId="p1" photos={photos} onUploaded={onUploaded} />);
  const input = screen.getByLabelText(/adicionar fotos/i) as HTMLInputElement;
  // applyAccept:false simula o usuário escolhendo "Todos os arquivos" no seletor, burlando o atributo accept.
  return { onUploaded, input, user: userEvent.setup({ applyAccept: false }) };
}

describe("PhotoUpload", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });
  afterEach(() => vi.unstubAllGlobals());

  it("aceita seleção múltipla", () => {
    const { input } = setup();
    expect(input.multiple).toBe(true);
  });

  it("envia todas as fotos selecionadas e recarrega a galeria uma única vez", async () => {
    fetchMock.mockImplementation(ok);
    const { input, user, onUploaded } = setup();

    await user.upload(input, [png("a.png"), png("b.png"), png("c.png")]);

    await waitFor(() => expect(screen.getAllByText("Enviada")).toHaveLength(3));
    expect(fetchMock).toHaveBeenCalledTimes(3);
    for (const [url, init] of fetchMock.mock.calls) {
      expect(url).toContain("/api/v1/properties/p1/photos");
      expect((init.body as FormData).get("file")).toBeInstanceOf(File);
      expect(init.headers).toEqual({ Authorization: "Bearer t" });
    }
    expect(onUploaded).toHaveBeenCalledTimes(1);
  });

  it("respeita o limite de 20: envia até o limite e avisa quantas foram ignoradas", async () => {
    fetchMock.mockImplementation(ok);
    const { input, user, onUploaded } = setup(Array.from({ length: 18 }, (_, i) => `http://x/${i}.png`));

    await user.upload(input, [png("1.png"), png("2.png"), png("3.png"), png("4.png")]);

    await waitFor(() => expect(screen.getAllByText("Enviada")).toHaveLength(2));
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(screen.getByText(/2 foto\(s\) ignorada\(s\).*20 fotos/)).toBeInTheDocument();
    expect(screen.getAllByText(/Limite de 20 fotos atingido/)).toHaveLength(2);
    expect(onUploaded).toHaveBeenCalledTimes(1);
  });

  it("um erro não cancela as demais e mostra a mensagem do backend no arquivo com falha", async () => {
    fetchMock
      .mockImplementationOnce(ok)
      .mockImplementationOnce(() =>
        Promise.resolve(new Response(JSON.stringify({ error: "Falha ao acessar o armazenamento de arquivos: AccessDenied (HTTP 403)" }), { status: 502 })),
      )
      .mockImplementationOnce(ok);
    const { input, user, onUploaded } = setup();

    await user.upload(input, [png("a.png"), png("b.png"), png("c.png")]);

    await waitFor(() => expect(screen.getAllByText("Enviada")).toHaveLength(2));
    expect(screen.getByRole("alert")).toHaveTextContent("AccessDenied (HTTP 403)");
    expect(onUploaded).toHaveBeenCalledTimes(1);
  });

  it("não recarrega a galeria quando nenhuma foto foi enviada", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response("{}", { status: 500 })));
    const { input, user, onUploaded } = setup();

    await user.upload(input, [png("a.png")]);

    expect(await screen.findByRole("alert")).toHaveTextContent("Falha ao enviar foto");
    expect(onUploaded).not.toHaveBeenCalled();
  });

  it("recusa na tela formato inválido e arquivo acima de 10 MB, sem chamar o backend", async () => {
    const { input, user, onUploaded } = setup();
    const pdf = new File(["x"], "doc.pdf", { type: "application/pdf" });
    const big = new File([new Uint8Array(MAX_PHOTO_BYTES + 1)], "grande.png", { type: "image/png" });

    await user.upload(input, [pdf, big]);

    expect(await screen.findByText(/Formato não permitido/)).toBeInTheDocument();
    expect(screen.getByText(/Acima do limite de 10 MB/)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
    expect(onUploaded).not.toHaveBeenCalled();
  });

  it("limita a concorrência de envios", async () => {
    let inFlight = 0;
    let peak = 0;
    const resolvers: (() => void)[] = [];
    fetchMock.mockImplementation(() => {
      inFlight += 1;
      peak = Math.max(peak, inFlight);
      return new Promise<Response>((resolve) => {
        resolvers.push(() => {
          inFlight -= 1;
          resolve(new Response("{}", { status: 200 }));
        });
      });
    });
    const { input, user } = setup();

    await user.upload(input, Array.from({ length: 7 }, (_, i) => png(`${i}.png`)));

    // Libera os envios conforme entram na fila, até terminar os 7.
    let released = 0;
    while (released < 7) {
      await waitFor(() => expect(resolvers.length).toBeGreaterThan(released));
      resolvers[released++]();
    }

    await waitFor(() => expect(screen.getAllByText("Enviada")).toHaveLength(7));
    expect(peak).toBe(UPLOAD_CONCURRENCY);
  });

  it("some com o botão quando o imóvel já tem 20 fotos", () => {
    render(<PhotoUpload propertyId="p1" photos={Array.from({ length: 20 }, (_, i) => `http://x/${i}.png`)} onUploaded={() => {}} />);
    expect(screen.queryByLabelText(/adicionar fotos/i)).not.toBeInTheDocument();
    expect(screen.getByText("20/20 fotos · JPG, PNG ou WebP até 10 MB cada")).toBeInTheDocument();
  });
});

describe("planUpload (função pura)", () => {
  const pdf = new File(["x"], "doc.pdf", { type: "application/pdf" });
  const big = new File([new Uint8Array(MAX_PHOTO_BYTES + 1)], "grande.png", { type: "image/png" });
  const names = (entries: { file: File }[]) => entries.map(({ file }) => file.name);

  it("aceita tudo quando cabe", () => {
    const plan = planUpload([png("a.png"), png("b.png")], 0);
    expect(names(plan.accepted)).toEqual(["a.png", "b.png"]);
    expect(plan.rejected).toEqual([]);
    expect(plan.overLimit).toBe(0);
  });

  it("separa formato inválido e arquivo grande, mantendo o motivo de cada um", () => {
    const plan = planUpload([png("ok.png"), pdf, big], 0);
    expect(names(plan.accepted)).toEqual(["ok.png"]);
    expect(plan.rejected.map(({ file, reason }) => [file.name, reason])).toEqual([
      ["doc.pdf", "Formato não permitido (use JPG, PNG ou WebP)"],
      ["grande.png", "Acima do limite de 10 MB"],
    ]);
  });

  it("corta no limite de fotos do imóvel e conta as ignoradas (só as válidas)", () => {
    const plan = planUpload([png("1.png"), png("2.png"), png("3.png"), pdf], MAX_PHOTOS - 1);
    expect(names(plan.accepted)).toEqual(["1.png"]);
    expect(plan.overLimit).toBe(2);
    expect(plan.rejected.map(({ reason }) => reason)).toEqual([
      "Formato não permitido (use JPG, PNG ou WebP)",
      `Limite de ${MAX_PHOTOS} fotos atingido`,
      `Limite de ${MAX_PHOTOS} fotos atingido`,
    ]);
  });

  it("com o imóvel cheio, não aceita nenhuma", () => {
    const plan = planUpload([png("a.png")], MAX_PHOTOS);
    expect(plan.accepted).toEqual([]);
    expect(plan.overLimit).toBe(1);
  });

  it("não altera os argumentos", () => {
    const files = [png("a.png"), pdf];
    planUpload(files, 0);
    expect(files.map((f) => f.name)).toEqual(["a.png", "doc.pdf"]);
  });
});

describe("photoKey (função pura)", () => {
  it("usa o último segmento do caminho e ignora a query string", () => {
    expect(photoKey("https://cdn.x/tenant/properties/p1/abc.png")).toBe("abc.png");
    expect(photoKey("https://cdn.x/p1/abc.png?sig=1/2")).toBe("abc.png");
  });
});

describe("PhotoUpload — exclusão", () => {
  const fetchMock = vi.fn();
  const urls = ["https://cdn.x/p1/a.png", "https://cdn.x/p1/b.png"];

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });
  afterEach(() => vi.unstubAllGlobals());

  it("sem permissão não mostra a ação de excluir", () => {
    render(<PhotoUpload propertyId="p1" photos={urls} onUploaded={() => {}} />);
    expect(screen.queryByRole("button", { name: /excluir foto/i })).not.toBeInTheDocument();
  });

  it("cancelar a confirmação não chama a API", async () => {
    const user = userEvent.setup();
    render(<PhotoUpload propertyId="p1" photos={urls} onUploaded={() => {}} canDelete />);

    await user.click(screen.getByRole("button", { name: "Excluir foto 2" }));
    await user.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(fetchMock).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("confirmar chama DELETE com a chave da foto e recarrega a galeria", async () => {
    fetchMock.mockImplementation(ok);
    const onUploaded = vi.fn();
    const user = userEvent.setup();
    render(<PhotoUpload propertyId="p1" photos={urls} onUploaded={onUploaded} canDelete />);

    await user.click(screen.getByRole("button", { name: "Excluir foto 2" }));
    await user.click(screen.getByRole("button", { name: "Excluir" }));

    await waitFor(() => expect(onUploaded).toHaveBeenCalledTimes(1));
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain("/api/v1/properties/p1/photos/b.png");
    expect(init.method).toBe("DELETE");
    expect(init.headers).toEqual({ Authorization: "Bearer t" });
  });

  it("falha na API mostra o erro e não recarrega", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ error: "Foto não encontrada" }), { status: 404 }));
    const onUploaded = vi.fn();
    const user = userEvent.setup();
    render(<PhotoUpload propertyId="p1" photos={urls} onUploaded={onUploaded} canDelete />);

    await user.click(screen.getByRole("button", { name: "Excluir foto 1" }));
    await user.click(screen.getByRole("button", { name: "Excluir" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Foto não encontrada");
    expect(onUploaded).not.toHaveBeenCalled();
  });
});
