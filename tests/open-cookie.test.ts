import * as anchor from "@coral-xyz/anchor";
import { Program } from "@coral-xyz/anchor";
import { PublicKey, SystemProgram } from "@solana/web3.js";
import { beforeAll, describe, expect, it } from "vitest";
import { OpenCookie } from "../target/types/open_cookie";
import {
  ERROR_CODES,
  MESSAGE_COUNT,
  expectAnchorError,
  parseCookieResult,
  parseReturnDataFromLogs,
  programDataPda,
} from "./helpers";

const ZERO_PUBKEY = new PublicKey(Buffer.alloc(32));

describe("open-cookie integration", () => {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.OpenCookie as Program<OpenCookie>;
  const admin = provider.wallet;

  const [configPda] = PublicKey.findProgramAddressSync(
    [Buffer.from("config")],
    program.programId,
  );

  const [treasuryVaultPda] = PublicKey.findProgramAddressSync(
    [Buffer.from("treasury-vault")],
    program.programId,
  );

  const [userProfilePda] = PublicKey.findProgramAddressSync(
    [Buffer.from("user"), admin.publicKey.toBuffer()],
    program.programId,
  );

  const programData = programDataPda(program.programId);

  const breakCookieAccounts = {
    user: admin.publicKey,
    config: configPda,
    userProfile: userProfilePda,
    treasuryVault: treasuryVaultPda,
    systemProgram: SystemProgram.programId,
  };

  async function simulateBreakCookie() {
    return program.methods
      .breakCookie()
      .accounts(breakCookieAccounts)
      .simulate();
  }

  async function breakCookie() {
    return program.methods
      .breakCookie()
      .accounts(breakCookieAccounts)
      .rpc();
  }

  async function updateConfig(maxCallsPerDay: number, priceLamports = 10_000) {
    return program.methods
      .updateConfig(ZERO_PUBKEY, new anchor.BN(priceLamports), maxCallsPerDay)
      .accounts({
        admin: admin.publicKey,
        config: configPda,
      })
      .rpc();
  }

  beforeAll(async () => {
    const configInfo = await provider.connection.getAccountInfo(configPda);
    if (!configInfo) {
      await program.methods
        .initializeConfig(ZERO_PUBKEY, new anchor.BN(10_000), 100)
        .accounts({
          admin: admin.publicKey,
          config: configPda,
          treasuryVault: treasuryVaultPda,
          program: program.programId,
          programData,
          systemProgram: SystemProgram.programId,
        })
        .rpc();
    }

    const profileInfo = await provider.connection.getAccountInfo(userProfilePda);
    if (!profileInfo) {
      await program.methods
        .initializeUser()
        .accounts({
          user: admin.publicKey,
          config: configPda,
          userProfile: userProfilePda,
          systemProgram: SystemProgram.programId,
        })
        .rpc();
    }
  });

  describe("config", () => {
    it("stores max_calls_per_day on-chain", async () => {
      const config = await program.account.config.fetch(configPda);
      expect(config.maxCallsPerDay).toBeGreaterThan(0);
      expect(config.priceLamports.toNumber()).toBeGreaterThanOrEqual(0);
    });

    it("rejects update_config when max_calls_per_day is zero", async () => {
      await expectAnchorError(
        program.methods
          .updateConfig(ZERO_PUBKEY, new anchor.BN(10_000), 0)
          .accounts({
            admin: admin.publicKey,
            config: configPda,
          })
          .rpc(),
        ERROR_CODES.InvalidConfig,
      );
    });

    it("updates max_calls_per_day and price through update_config", async () => {
      await updateConfig(50, 7_500);

      const updated = await program.account.config.fetch(configPda);
      expect(updated.maxCallsPerDay).toBe(50);
      expect(updated.priceLamports.toNumber()).toBe(7_500);

      await updateConfig(100, 10_000);

      const restored = await program.account.config.fetch(configPda);
      expect(restored.maxCallsPerDay).toBe(100);
      expect(restored.priceLamports.toNumber()).toBe(10_000);
    });
  });

  describe("break_cookie", () => {
    it("returns message_index in the 0..999 range via return data", async () => {
      const simulation = await simulateBreakCookie();
      const returnData = parseReturnDataFromLogs(simulation.raw, program.programId);

      expect(returnData).not.toBeNull();

      const result = parseCookieResult(returnData!);
      expect(result.messageIndex).toBeGreaterThanOrEqual(0);
      expect(result.messageIndex).toBeLessThan(MESSAGE_COUNT);
    });

    it("returns calls_today as u16-compatible value in return data", async () => {
      const profileBefore = await program.account.userProfile.fetch(userProfilePda);
      const simulation = await simulateBreakCookie();
      const returnData = parseReturnDataFromLogs(simulation.raw, program.programId);
      const result = parseCookieResult(returnData!);

      expect(result.callsToday).toBe(profileBefore.callsToday + 1);
      expect(result.totalCalls).toBe(profileBefore.totalCalls + 1);
    });

    it("enforces the configurable daily limit from config", async () => {
      const profile = await program.account.userProfile.fetch(userProfilePda);
      const atLimit = profile.callsToday;

      await updateConfig(atLimit);

      await expectAnchorError(breakCookie(), ERROR_CODES.DailyLimitReached);

      await updateConfig(atLimit + 1);
      await breakCookie();

      const after = await program.account.userProfile.fetch(userProfilePda);
      expect(after.callsToday).toBe(atLimit + 1);
      expect(after.totalCalls).toBe(profile.totalCalls + 1);

      await updateConfig(100);
    });

    it("collects the configured protocol fee into the treasury vault", async () => {
      const config = await program.account.config.fetch(configPda);
      const vaultBefore = await provider.connection.getBalance(treasuryVaultPda);
      const profileBefore = await program.account.userProfile.fetch(userProfilePda);

      await breakCookie();

      const vaultAfter = await provider.connection.getBalance(treasuryVaultPda);
      expect(vaultAfter - vaultBefore).toBe(config.priceLamports.toNumber());

      const profileAfter = await program.account.userProfile.fetch(userProfilePda);
      expect(profileAfter.totalCalls).toBe(profileBefore.totalCalls + 1);
      expect(profileAfter.callsToday).toBe(profileBefore.callsToday + 1);
    });
  });

  describe("treasury", () => {
    it("withdraws lamports to a destination chosen by admin", async () => {
      const destination = anchor.web3.Keypair.generate();

      const vaultBefore = await provider.connection.getBalance(treasuryVaultPda);
      const rentMin = await provider.connection.getMinimumBalanceForRentExemption(0);
      const maxWithdraw = vaultBefore - rentMin;
      const withdrawAmount = Math.min(5_000, maxWithdraw);

      expect(withdrawAmount).toBeGreaterThan(0);

      await program.methods
        .withdrawTreasury({ lamports: new anchor.BN(withdrawAmount) })
        .accounts({
          admin: admin.publicKey,
          config: configPda,
          treasuryVault: treasuryVaultPda,
          destination: destination.publicKey,
          systemProgram: SystemProgram.programId,
        })
        .rpc();

      const vaultAfter = await provider.connection.getBalance(treasuryVaultPda);
      expect(vaultBefore - vaultAfter).toBe(withdrawAmount);
    });
  });
});
