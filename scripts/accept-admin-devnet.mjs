#!/usr/bin/env node
import * as anchor from "@coral-xyz/anchor";
import { configPda } from "./cluster-common.mjs";

const provider = anchor.AnchorProvider.env();
anchor.setProvider(provider);

const program = anchor.workspace.OpenCookie;
const pendingAdmin = provider.wallet;

const sig = await program.methods
  .acceptAdmin()
  .accounts({
    pendingAdmin: pendingAdmin.publicKey,
    config: configPda(),
  })
  .rpc();

console.log("accept_admin tx:", sig);
