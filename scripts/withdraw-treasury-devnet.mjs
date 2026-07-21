#!/usr/bin/env node
import * as anchor from "@coral-xyz/anchor";
import BN from "bn.js";
import { PublicKey, SystemProgram } from "@solana/web3.js";
import {
  configPda,
  formatLamports,
  maxSafeWithdrawLamports,
  treasuryPda,
} from "./cluster-common.mjs";

const provider = anchor.AnchorProvider.env();
anchor.setProvider(provider);

const program = anchor.workspace.OpenCookie;
const admin = provider.wallet;

const command = process.argv[2] ?? "status";

if (command === "status") {
  const { balance, rentMin, max } = await maxSafeWithdrawLamports(provider.connection);
  console.log("Treasury PDA:", treasuryPda().toBase58());
  console.log("Balance:", formatLamports(balance));
  console.log("Rent floor:", formatLamports(rentMin));
  console.log("Max withdraw:", formatLamports(max));
  process.exit(0);
}

if (command === "withdraw") {
  const destination = new PublicKey(process.argv[3]);
  const lamports = BigInt(process.argv[4] ?? "0");
  if (lamports <= 0n) {
    console.error("Usage: node scripts/withdraw-treasury-devnet.mjs withdraw <destination> <lamports>");
    process.exit(1);
  }

  const sig = await program.methods
    .withdrawTreasury({ lamports: new BN(lamports.toString()) })
    .accounts({
      admin: admin.publicKey,
      config: configPda(),
      treasuryVault: treasuryPda(),
      destination,
      systemProgram: SystemProgram.programId,
    })
    .rpc();

  console.log("withdraw_treasury tx:", sig);
  process.exit(0);
}

console.error("Usage: status | withdraw <destination> <lamports>");
