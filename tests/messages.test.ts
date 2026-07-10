import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const MESSAGES_PATH = resolve(
  __dirname,
  "../android/app/src/main/assets/messages_en.json",
);

describe("messages_en.json", () => {
  const messages = JSON.parse(readFileSync(MESSAGES_PATH, "utf8")) as string[];

  it("contains exactly 1000 messages", () => {
    expect(messages).toHaveLength(1000);
  });

  it("contains only unique non-empty English strings", () => {
    expect(new Set(messages).size).toBe(1000);
    for (const message of messages) {
      expect(typeof message).toBe("string");
      expect(message.trim().length).toBeGreaterThan(0);
    }
  });
});
