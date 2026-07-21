import { PublicKey, SystemProgram } from "@solana/web3.js";

export const PROGRAM_ID = new PublicKey("CooknomesWJ3KdJUUYfgXBycS19hqDNS9riBavo2Gfuf");

export function configPda() {
  return PublicKey.findProgramAddressSync([Buffer.from("config")], PROGRAM_ID)[0];
}

export function treasuryPda() {
  return PublicKey.findProgramAddressSync([Buffer.from("treasury-vault")], PROGRAM_ID)[0];
}

export function programDataPda() {
  const bpfLoader = new PublicKey("BPFLoaderUpgradeab1e11111111111111111111111");
  return PublicKey.findProgramAddressSync([PROGRAM_ID.toBuffer()], bpfLoader)[0];
}

export { SystemProgram };

export function formatLamports(lamports) {
  return `${Number(lamports) / 1e9} SOL (${lamports} lamports)`;
}

export async function treasuryBalance(connection) {
  return BigInt(await connection.getBalance(treasuryPda()));
}

export async function maxSafeWithdrawLamports(connection) {
  const balance = await treasuryBalance(connection);
  const rentMin = BigInt(await connection.getMinimumBalanceForRentExemption(0));
  const max = balance > rentMin ? balance - rentMin : 0n;
  return { balance, rentMin, max };
}
