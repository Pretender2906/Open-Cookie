#!/usr/bin/env node
import * as anchor from "@coral-xyz/anchor";
import { PublicKey, SystemProgram } from "@solana/web3.js";
import {
  configPda,
  programDataPda,
  treasuryPda,
} from "./cluster-common.mjs";

const provider = anchor.AnchorProvider.env();
anchor.setProvider(provider);

const program = anchor.workspace.OpenCookie;
const admin = provider.wallet;

const DEFAULT_MAX_CALLS_PER_DAY = 100;

const arg1 = process.argv[2];
const arg2 = process.argv[3];
const arg3 = process.argv[4];

if (!arg1) {
  console.error("Usage:");
  console.error("  node scripts/init-config-devnet.mjs <price_lamports> [max_calls_per_day]");
  console.error("  node scripts/init-config-devnet.mjs <pending_admin_pubkey> <price_lamports> [max_calls_per_day]");
  process.exit(1);
}

const ZERO = new PublicKey(Buffer.alloc(32));
let pendingAdmin = ZERO;
let priceLamports;
let maxCallsPerDay = DEFAULT_MAX_CALLS_PER_DAY;

if (arg2 && !/^\d+$/.test(arg1)) {
  pendingAdmin = new PublicKey(arg1);
  priceLamports = new anchor.BN(arg2);
  if (arg3) maxCallsPerDay = Number(arg3);
} else {
  priceLamports = new anchor.BN(arg1);
  if (arg2) maxCallsPerDay = Number(arg2);
}

console.log("Initializing config at", configPda().toBase58());
console.log("Treasury vault:", treasuryPda().toBase58());
console.log("Pending admin:", pendingAdmin.toBase58());
console.log("Price:", priceLamports.toString(), "lamports");
console.log("Max calls per day:", maxCallsPerDay);

const sig = await program.methods
  .initializeConfig(pendingAdmin, priceLamports, maxCallsPerDay)
  .accounts({
    admin: admin.publicKey,
    config: configPda(),
    treasuryVault: treasuryPda(),
    program: program.programId,
    programData: programDataPda(),
    systemProgram: SystemProgram.programId,
  })
  .rpc();

console.log("Signature:", sig);
