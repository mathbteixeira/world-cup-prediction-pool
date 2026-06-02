import { api, configureApiClient } from "./client";

describe("api client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("sends bearer tokens to protected endpoints", async () => {
    configureApiClient({ getToken: () => "abc", onUnauthorized: vi.fn() });
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await api.listPools();

    const headers = fetchMock.mock.calls[0][1]?.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer abc");
  });

  it("surfaces backend validation errors", async () => {
    configureApiClient({ getToken: () => null, onUnauthorized: vi.fn() });
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "Email is already in use", statusCode: 409 }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(api.register({ username: "alice", email: "alice@example.com", password: "password123" })).rejects.toThrow(
      "Email is already in use",
    );
  });
});
