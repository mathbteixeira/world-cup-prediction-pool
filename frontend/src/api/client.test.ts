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

  it("lists tournaments from the tournament discovery endpoint", async () => {
    configureApiClient({ getToken: () => "abc", onUnauthorized: vi.fn() });
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await api.listTournaments();

    expect(fetchMock.mock.calls[0][0]).toBe("http://localhost:8080/api/v1/tournaments");
  });

  it("creates a managed participant and submits their prediction", async () => {
    configureApiClient({ getToken: () => "abc", onUnauthorized: vi.fn() });
    const poolId = "pool-1";
    const participantId = "participant-1";
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ participantId, poolId, name: "Grandma" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ predictionId: "prediction-1", poolId, matchId: "match-1", homeScore: 2, awayScore: 1 }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );

    await api.createManagedParticipant(poolId, { name: "Grandma" });
    await api.submitManagedParticipantPrediction(poolId, participantId, { homeScore: 2, awayScore: 1 });

    expect(fetchMock.mock.calls[0][0]).toBe("http://localhost:8080/api/v1/pools/pool-1/managed-participants");
    expect(fetchMock.mock.calls[0][1]?.method).toBe("POST");
    expect(fetchMock.mock.calls[1][0]).toBe(
      "http://localhost:8080/api/v1/pools/pool-1/managed-participants/participant-1/prediction",
    );
    expect(fetchMock.mock.calls[1][1]?.method).toBe("PUT");
  });
});
