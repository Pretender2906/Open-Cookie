#!/usr/bin/env node
import * as anchor from "@coral-xyz/anchor";
import { PublicKey } from "@solana/web3.js";
import { configPda } from "./cluster-common.mjs";

const provider = anchor.AnchorProvider.env();
anchor.setProvider(provider);

const program = anchor.workspace.OpenCookie;
const admin = provider.wallet;

const pendingAdminArg = process.argv[2];
const priceArg = process.argv[3];

if (!priceArg) {
  console.error("Usage: node scripts/update-config-devnet.mjs [pending_admin_pubkey] <price_lamports>");
  process.exit(1);
}

const ZERO = new PublicKey(Buffer.alloc(32));
const pendingAdmin = pendingAdminArg && !/^\d+$/.test(pendingAdminArg)
  ? new PublicKey(pendingAdminArg)
  : ZERO;
const priceLamports = new anchor.BN(
  pendingAdminArg && /^\d+$/.test(pendingAdminArg) ? pendingAdminArg : priceArg,
);

const sig = await program.methods
  .updateConfig(pendingAdmin, priceLamports)
  .accounts({
    admin: admin.publicKey,
    config: configPda(),
  })
  .rpc();

console.log("update_config tx:", sig);
