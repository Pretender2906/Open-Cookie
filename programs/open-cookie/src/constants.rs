use anchor_lang::prelude::*;

pub const CONFIG_SEED: &[u8] = b"config";
pub const USER_SEED: &[u8] = b"user";
pub const TREASURY_VAULT_SEED: &[u8] = b"treasury-vault";

pub const DEFAULT_MAX_CALLS_PER_DAY: u16 = 100;
pub const MESSAGE_COUNT: u16 = 1000;

pub const DEFAULT_PENDING_ADMIN: Pubkey = Pubkey::new_from_array([0u8; 32]);

pub const CONFIG_LEN: usize = 8 + 32 + 32 + 8 + 2 + 1 + 1;
pub const USER_PROFILE_LEN: usize = 8 + 32 + 4 + 4 + 2 + 1;
