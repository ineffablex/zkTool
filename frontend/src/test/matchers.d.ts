/// <reference types="vitest" />

interface CustomMatchers<R = unknown> {
  toBe(expected: unknown): R;
  toEqual(expected: unknown): R;
  toBeGreaterThan(expected: number): R;
  toContain(expected: unknown): R;
  toHaveLength(expected: number): R;
  toBeTruthy(): R;
  toBeFalsy(): R;
  toHaveBeenCalled(): R;
  toHaveBeenCalledWith(...args: unknown[]): R;
  toBeNull(): R;
  toBeDefined(): R;
  toBeUndefined(): R;
  toBeNaN(): R;
  toMatch(pattern: string | RegExp): R;
  toMatchObject(object: unknown): R;
  toThrow(message?: string | RegExp): R;
}

declare module 'vitest' {
  interface Assertion extends CustomMatchers {}
  interface AsymmetricMatchersContaining extends CustomMatchers {}

  interface ExpectStatic {
    (actual: unknown): Assertion;
    extend(matchers: Record<string, unknown>): void;
    any(): unknown;
    objectContaining(expected: object): unknown;
    stringContaining(expected: string): unknown;
    arrayContaining(expected: unknown[]): unknown;
  }
}

declare module '@vue/test-utils' {
  export * from '@vue/test-utils';
}

declare module '@pinia/testing' {
  export * from '@pinia/testing';
}

declare module 'jest' {
  interface SpyInstance {
    mockReset(): void;
    mockRestore(): void;
    mockClear(): void;
    mockImplementation(fn: (...args: any[]) => any): this;
    mockReturnValue(value: any): this;
    mockReturnValueOnce(value: any): this;
  }
  interface Mock extends Function {
    [key: string]: any;
  }
}

export {};