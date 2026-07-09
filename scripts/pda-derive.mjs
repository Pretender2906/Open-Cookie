#!/usr/bin/env node
import { PublicKey } from "@solana/web3.js";
import { PROGRAM_ID } from "./cluster-common.mjs";

function pda(seeds) {
  return PublicKey.findProgramAddressSync(seeds, PROGRAM_ID);
}

const [config] = pda([Buffer.from("config")]);
const [treasury] = pda([Buffer.from("treasury-vault")]);

console.log("Program ID:", PROGRAM_ID.toBase58());
console.log("Config PDA:", config.toBase58());
console.log("Treasury vault PDA:", treasury.toBase58());

if (process.argv[2]) {
  const user = new PublicKey(process.argv[2]);
  const [profile] = pda([Buffer.from("user"), user.toBuffer()]);
  console.log("User profile PDA:", profile.toBase58());
}
