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

const program = anchor.workspace.FortuneButton;
const admin = provider.wallet;

const arg1 = process.argv[2];
const arg2 = process.argv[3];

if (!arg1) {
  console.error("Usage:");
  console.error("  node scripts/init-config-devnet.mjs <price_lamports>");
  console.error("  node scripts/init-config-devnet.mjs <pending_admin_pubkey> <price_lamports>");
  process.exit(1);
}

const ZERO = new PublicKey(Buffer.alloc(32));
let pendingAdmin = ZERO;
let priceLamports;

if (arg2) {
  pendingAdmin = new PublicKey(arg1);
  priceLamports = new anchor.BN(arg2);
} else {
  priceLamports = new anchor.BN(arg1);
}

console.log("Initializing config at", configPda().toBase58());
console.log("Treasury vault:", treasuryPda().toBase58());
console.log("Pending admin:", pendingAdmin.toBase58());
console.log("Price:", priceLamports.toString(), "lamports");

const sig = await program.methods
  .initializeConfig(pendingAdmin, priceLamports)
  .accounts({
    admin: admin.publicKey,
    config: configPda(),
    treasuryVault: treasuryPda(),
    programData: programDataPda(),
    systemProgram: SystemProgram.programId,
  })
  .rpc();

console.log("Signature:", sig);
