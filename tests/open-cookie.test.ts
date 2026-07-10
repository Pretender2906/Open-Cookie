import * as anchor from "@coral-xyz/anchor";
import { Program } from "@coral-xyz/anchor";
import {
  LAMPORTS_PER_SOL,
  PublicKey,
  SystemProgram,
} from "@solana/web3.js";
import { beforeAll, describe, expect, it } from "vitest";
import { OpenCookie } from "../target/types/open_cookie";

const ZERO_PUBKEY = new PublicKey(Buffer.alloc(32));
const BPF_LOADER = new PublicKey("BPFLoaderUpgradeab1e11111111111111111111111");

function programDataPda(programId: PublicKey): PublicKey {
  return PublicKey.findProgramAddressSync([programId.toBuffer()], BPF_LOADER)[0];
}

describe("open-cookie", () => {
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

  beforeAll(async () => {
    const sig = await provider.connection.requestAirdrop(
      admin.publicKey,
      2 * LAMPORTS_PER_SOL,
    );
    await provider.connection.confirmTransaction(sig);
  });

  it("initializes config with treasury vault", async () => {
    await program.methods
      .initializeConfig(ZERO_PUBKEY, new anchor.BN(10_000))
      .accounts({
        admin: admin.publicKey,
        config: configPda,
        treasuryVault: treasuryVaultPda,
        programData,
        systemProgram: SystemProgram.programId,
      })
      .rpc();

    const config = await program.account.config.fetch(configPda);
    expect(config.adminAuthority.toBase58()).toBe(admin.publicKey.toBase58());
    expect(config.pendingAdmin.toBase58()).toBe(ZERO_PUBKEY.toBase58());
    expect(config.priceLamports.toNumber()).toBe(10_000);

    const vaultBalance = await provider.connection.getBalance(treasuryVaultPda);
    expect(vaultBalance).toBeGreaterThan(0);
  });

  it("initializes user profile", async () => {
    await program.methods
      .initializeUser()
      .accounts({
        user: admin.publicKey,
        config: configPda,
        userProfile: userProfilePda,
        systemProgram: SystemProgram.programId,
      })
      .rpc();

    const profile = await program.account.userProfile.fetch(userProfilePda);
    expect(profile.owner.toBase58()).toBe(admin.publicKey.toBase58());
    expect(profile.totalCalls).toBe(0);
  });

  it("returns message index and collects fee to vault", async () => {
    const before = await provider.connection.getBalance(treasuryVaultPda);

    const tx = await program.methods
      .breakCookie()
      .accounts({
        user: admin.publicKey,
        config: configPda,
        userProfile: userProfilePda,
        treasuryVault: treasuryVaultPda,
        systemProgram: SystemProgram.programId,
      })
      .simulate();

    expect(tx.returnData).toBeDefined();

    await program.methods
      .breakCookie()
      .accounts({
        user: admin.publicKey,
        config: configPda,
        userProfile: userProfilePda,
        treasuryVault: treasuryVaultPda,
        systemProgram: SystemProgram.programId,
      })
      .rpc();

    const after = await provider.connection.getBalance(treasuryVaultPda);
    expect(after - before).toBe(10_000);

    const profile = await program.account.userProfile.fetch(userProfilePda);
    expect(profile.totalCalls).toBe(1);
  });

  it("withdraws treasury to destination", async () => {
    const destination = anchor.web3.Keypair.generate();
    const sig = await provider.connection.requestAirdrop(destination.publicKey, 0);
    await provider.connection.confirmTransaction(sig);

    const before = await provider.connection.getBalance(treasuryVaultPda);

    await program.methods
      .withdrawTreasury({ lamports: new anchor.BN(5_000) })
      .accounts({
        admin: admin.publicKey,
        config: configPda,
        treasuryVault: treasuryVaultPda,
        destination: destination.publicKey,
        systemProgram: SystemProgram.programId,
      })
      .rpc();

    const after = await provider.connection.getBalance(treasuryVaultPda);
    expect(before - after).toBe(5_000);
  });
});
