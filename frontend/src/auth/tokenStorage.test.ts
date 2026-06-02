import { clearStoredToken, getStoredToken, storeToken } from "./tokenStorage";

describe("tokenStorage", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("stores and clears the local portfolio JWT", () => {
    storeToken("jwt-token");

    expect(getStoredToken()).toBe("jwt-token");

    clearStoredToken();

    expect(getStoredToken()).toBeNull();
  });
});
