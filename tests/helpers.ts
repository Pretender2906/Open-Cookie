import { expect } from "vitest";
import { PublicKey } from "@solana/web3.js";

export const MESSAGE_COUNT = 1000;

export const ERROR_CODES = {
  InvalidConfig: 6005,
  DailyLimitReached: 6006,
} as const;

export function programDataPda(programId: PublicKey): PublicKey {
  const BPF_LOADER = new PublicKey("BPFLoaderUpgradeab1e11111111111111111111111");
  return PublicKey.findProgramAddressSync([programId.toBuffer()], BPF_LOADER)[0];
}

export function findReturnDataLog(
  logs: readonly string[],
  programId: PublicKey,
): string | undefined {
  const prefix = `Program return: ${programId.toBase58()} `;
  return logs.find((line) => line.startsWith(prefix));
}

export function parseReturnDataFromLogs(
  logs: readonly string[],
  programId: PublicKey,
): Buffer | null {
  const line = findReturnDataLog(logs, programId);
  if (!line) return null;
  const base64 = line.slice(prefixLength(programId));
  return Buffer.from(base64, "base64");
}

function prefixLength(programId: PublicKey): number {
  return `Program return: ${programId.toBase58()} `.length;
}

export function parseCookieResult(data: Buffer): {
  messageIndex: number;
  totalCalls: number;
  callsToday: number;
} {
  if (data.length < 8) {
    throw new Error(`CookieResult must be at least 8 bytes, got ${data.length}`);
  }
  return {
    messageIndex: data.readUInt16LE(0),
    totalCalls: data.readUInt32LE(2),
    callsToday: data.readUInt16LE(6),
  };
}

export async function expectAnchorError(
  promise: Promise<unknown>,
  code: number,
): Promise<void> {
  try {
    await promise;
    throw new Error(`Expected Anchor error ${code}, but instruction succeeded`);
  } catch (err: unknown) {
    const anchorErr = err as {
      error?: { errorCode?: { code?: number } };
      code?: number;
    };
    const actual = anchorErr.error?.errorCode?.code ?? anchorErr.code;
    if (actual === undefined) {
      throw err;
    }
    expect(actual).toBe(code);
  }
}
