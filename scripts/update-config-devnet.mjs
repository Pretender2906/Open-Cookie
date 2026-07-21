#!/usr/bin/env node
import * as anchor from "@coral-xyz/anchor";
import BN from "bn.js";
import { PublicKey } from "@solana/web3.js";
import { configPda } from "./cluster-common.mjs";

const DEFAULT_MAX_CALLS_PER_DAY = 100;

const provider = anchor.AnchorProvider.env();
anchor.setProvider(provider);

const program = anchor.workspace.OpenCookie;
const admin = provider.wallet;

const pendingAdminArg = process.argv[2];
const priceArg = process.argv[3];
const maxCallsArg = process.argv[4];

if (!priceArg) {
  console.error("Usage: node scripts/update-config-devnet.mjs [pending_admin_pubkey] <price_lamports> [max_calls_per_day]");
  process.exit(1);
}

const ZERO = new PublicKey(Buffer.alloc(32));
const pendingAdmin = pendingAdminArg && !/^\d+$/.test(pendingAdminArg)
  ? new PublicKey(pendingAdminArg)
  : ZERO;
const priceLamports = new BN(
  pendingAdminArg && /^\d+$/.test(pendingAdminArg) ? pendingAdminArg : priceArg,
);
const maxCallsPerDay = Number(
  pendingAdminArg && /^\d+$/.test(pendingAdminArg)
    ? (priceArg ?? DEFAULT_MAX_CALLS_PER_DAY)
    : (maxCallsArg ?? DEFAULT_MAX_CALLS_PER_DAY),
);

const sig = await program.methods
  .updateConfig(pendingAdmin, priceLamports, maxCallsPerDay)
  .accounts({
    admin: admin.publicKey,
    config: configPda(),
  })
  .rpc();

console.log("update_config tx:", sig);
