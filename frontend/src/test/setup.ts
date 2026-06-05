import "@testing-library/jest-dom/vitest";

const testGlobal = globalThis as typeof globalThis & {
  window?: typeof globalThis;
  localStorage?: Storage;
};

if (!testGlobal.window) {
  Object.defineProperty(globalThis, "window", {
    configurable: true,
    value: globalThis,
  });
}

if (!testGlobal.localStorage) {
  const store = new Map<string, string>();
  const localStorage: Storage = {
    get length() {
      return store.size;
    },
    clear: () => store.clear(),
    getItem: (key) => store.get(key) ?? null,
    key: (index) => [...store.keys()][index] ?? null,
    removeItem: (key) => store.delete(key),
    setItem: (key, value) => store.set(key, value),
  };

  Object.defineProperty(globalThis, "localStorage", {
    configurable: true,
    value: localStorage,
  });
  Object.defineProperty(testGlobal.window!, "localStorage", {
    configurable: true,
    value: localStorage,
  });
}
