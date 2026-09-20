import { act, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PhotoUpload, moveItem } from "./PhotoUpload";

vi.mock("@clerk/nextjs", () => ({ useAuth: () => ({ getToken: async () => "t" }) }));

// O jsdom não tem layout, então o arraste real não dispara: capturamos o onDragEnd do DndContext e o chamamos direto.
let dragEnd: (event: { active: { id: string }; over: { id: string } | null }) => void = () => {};
vi.mock("@dnd-kit/core", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@dnd-kit/core")>();
  return {
    ...actual,
    DndContext: (props: React.ComponentProps<typeof actual.DndContext>) => {
      dragEnd = props.onDragEnd as typeof dragEnd;
      return <actual.DndContext {...props} />;
    },
  };
});

const photos = ["http://x/p/a.png", "http://x/p/b.png", "http://x/p/c.png"];
const drop = (from: string, to: string | null) => act(() => dragEnd({ active: { id: from }, over: to ? { id: to } : null }));

describe("moveItem (função pura)", () => {
  it("move para frente e para trás", () => {
    expect(moveItem(["a", "b", "c", "d"], 0, 2)).toEqual(["b", "c", "a", "d"]);
    expect(moveItem(["a", "b", "c", "d"], 3, 1)).toEqual(["a", "d", "b", "c"]);
  });

  it("índice igual ou fora da lista devolve uma cópia sem alterações", () => {
    const items = ["a", "b"];
    expect(moveItem(items, 1, 1)).toEqual(items);
    expect(moveItem(items, -1, 0)).toEqual(items);
    expect(moveItem(items, 0, 5)).toEqual(items);
    expect(moveItem(items, 0, 5)).not.toBe(items);
  });

  it("não altera o argumento", () => {
    const items = Object.freeze(["a", "b", "c"]);
    expect(moveItem(items, 0, 2)).toEqual(["b", "c", "a"]);
  });
});

describe("PhotoUpload — reordenação", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });
  afterEach(() => vi.unstubAllGlobals());

  const setup = (canReorder = true) => {
    const onUploaded = vi.fn();
    render(<PhotoUpload propertyId="p1" photos={photos} onUploaded={onUploaded} canReorder={canReorder} />);
    return { onUploaded };
  };

  it("sem permissão não mostra a alça de reordenar", () => {
    setup(false);
    expect(screen.queryByRole("button", { name: /reordenar foto/i })).not.toBeInTheDocument();
  });

  it("com uma única foto não há o que reordenar", () => {
    render(<PhotoUpload propertyId="p1" photos={[photos[0]]} onUploaded={vi.fn()} canReorder />);
    expect(screen.queryByRole("button", { name: /reordenar foto/i })).not.toBeInTheDocument();
  });

  it("mostra uma alça por foto (acessível por rótulo) e marca só a primeira como Capa", () => {
    setup();
    expect(screen.getAllByRole("button", { name: /reordenar foto/i })).toHaveLength(3);
    expect(screen.getAllByText("Capa")).toHaveLength(1);
  });

  it("soltar em outra posição envia as chaves na nova ordem e recarrega a galeria", async () => {
    fetchMock.mockResolvedValue(new Response("{}", { status: 200 }));
    const { onUploaded } = setup();

    drop(photos[2], photos[0]);

    await waitFor(() => expect(onUploaded).toHaveBeenCalledTimes(1));
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain("/api/v1/properties/p1/photos/order");
    expect(init.method).toBe("PUT");
    expect(init.headers).toMatchObject({ Authorization: "Bearer t", "Content-Type": "application/json" });
    expect(JSON.parse(init.body)).toEqual({ keys: ["c.png", "a.png", "b.png"] });
  });

  it("aplica a nova ordem na tela antes da resposta do servidor", async () => {
    fetchMock.mockReturnValue(new Promise(() => {}));
    setup();

    drop(photos[2], photos[0]);

    const srcs = () => screen.getAllByAltText("Foto do imóvel").map((img) => img.getAttribute("src"));
    await waitFor(() => expect(srcs()).toEqual([photos[2], photos[0], photos[1]]));
  });

  it("se o servidor recusa, volta a ordem anterior e mostra o erro", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ error: "Ordem inválida" }), { status: 422 }));
    const { onUploaded } = setup();

    drop(photos[2], photos[0]);

    expect(await screen.findByRole("alert")).toHaveTextContent("Ordem inválida");
    expect(screen.getAllByAltText("Foto do imóvel").map((img) => img.getAttribute("src"))).toEqual(photos);
    expect(onUploaded).not.toHaveBeenCalled();
  });

  it("se a rede falha, volta a ordem anterior e mostra mensagem", async () => {
    fetchMock.mockRejectedValue(new Error("Failed to fetch"));
    setup();

    drop(photos[1], photos[2]);

    expect(await screen.findByRole("alert")).toHaveTextContent("Failed to fetch");
    expect(screen.getAllByAltText("Foto do imóvel").map((img) => img.getAttribute("src"))).toEqual(photos);
  });

  it("soltar sobre si mesmo ou fora da galeria não chama o backend", () => {
    setup();
    drop(photos[0], photos[0]);
    drop(photos[0], null);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
