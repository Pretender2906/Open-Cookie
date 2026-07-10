import { describe, expect, it } from "vitest";
import { MESSAGE_COUNT, parseCookieResult } from "./helpers";

describe("cookie return data", () => {
  it("decodes message_index, total_calls and u16 calls_today", () => {
    const data = Buffer.alloc(8);
    data.writeUInt16LE(999, 0);
    data.writeUInt32LE(42, 2);
    data.writeUInt16LE(256, 6);

    const parsed = parseCookieResult(data);

    expect(parsed.messageIndex).toBe(999);
    expect(parsed.totalCalls).toBe(42);
    expect(parsed.callsToday).toBe(256);
  });

  it("supports message indices across the full 1000-message range", () => {
    for (const index of [0, 1, 999]) {
      const data = Buffer.alloc(8);
      data.writeUInt16LE(index, 0);
      data.writeUInt32LE(1, 2);
      data.writeUInt16LE(1, 6);

      expect(parseCookieResult(data).messageIndex).toBe(index);
      expect(parseCookieResult(data).messageIndex).toBeLessThan(MESSAGE_COUNT);
    }
  });
});
