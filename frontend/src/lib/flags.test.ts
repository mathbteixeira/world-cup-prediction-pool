import { flagForFifaCode } from "./flags";

describe("flagForFifaCode", () => {
  it("returns the England flag for ENG", () => {
    expect(flagForFifaCode("ENG")).toBe("\u{1F3F4}\u{E0067}\u{E0062}\u{E0065}\u{E006E}\u{E0067}\u{E007F}");
  });
});
