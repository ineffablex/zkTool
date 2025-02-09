/// <reference types="vitest/globals" />

declare module '@vue/test-utils' {
  export * from '@vue/test-utils'
}

declare module '@pinia/testing' {
  export * from '@pinia/testing'
}

declare type Expect = {
  <T = any>(actual: T): {
    toBe(expected: T): void;
    toEqual(expected: any): void;
    toContain(expected: any): void;
    toHaveLength(expected: number): void;
    toBeTruthy(): void;
    toBeFalsy(): void;
    toBeUndefined(): void;
    toBeNull(): void;
    toBeGreaterThan(expected: number): void;
    toBeLessThan(expected: number): void;
    toHaveBeenCalled(): void;
    toHaveBeenCalledWith(...args: any[]): void;
    not: Expect;
  };
  extend(matchers: Record<string, unknown>): void;
  any(): any;
  objectContaining(object: Record<string, any>): any;
  stringContaining(str: string): any;
  arrayContaining(arr: any[]): any;
}

declare global {
  const expect: Expect;
  const describe: (name: string, callback: () => void) => void;
  const it: (name: string, callback: () => void | Promise<void>) => void;
  const test: (name: string, callback: () => void | Promise<void>) => void;
  const beforeAll: (callback: () => void | Promise<void>) => void;
  const beforeEach: (callback: () => void | Promise<void>) => void;
  const afterAll: (callback: () => void | Promise<void>) => void;
  const afterEach: (callback: () => void | Promise<void>) => void;
  const vi: {
    fn: (implementation?: (...args: any[]) => any) => jest.Mock;
    spyOn: (object: any, method: string) => jest.SpyInstance;
    mock: (moduleName: string, factory?: () => any) => jest.Mock;
    clearAllMocks: () => void;
    resetAllMocks: () => void;
    restoreAllMocks: () => void;
  };
  
  namespace jest {
    interface Mock extends Function {
      mockReset(): void;
      mockRestore(): void;
      mockImplementation(fn: (...args: any[]) => any): this;
      mockReturnValue(value: any): this;
      mockReturnValueOnce(value: any): this;
    }

    interface SpyInstance {
      mockReset(): void;
      mockRestore(): void;
      mockImplementation(fn: (...args: any[]) => any): this;
      mockReturnValue(value: any): this;
      mockReturnValueOnce(value: any): this;
    }
  }
}

export {};