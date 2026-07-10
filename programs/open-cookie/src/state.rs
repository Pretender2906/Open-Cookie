use anchor_lang::prelude::*;

use crate::constants::DEFAULT_PENDING_ADMIN;

#[account]
pub struct Config {
    pub admin_authority: Pubkey,
    pub pending_admin: Pubkey,
    pub price_lamports: u64,
    pub treasury_bump: u8,
    pub config_bump: u8,
}

impl Config {
    pub fn clear_pending_admin(&mut self) {
        self.pending_admin = DEFAULT_PENDING_ADMIN;
    }
}

#[account]
pub struct UserProfile {
    pub owner: Pubkey,
    pub total_calls: u32,
    pub last_day: i32,
    pub calls_today: u8,
    pub bump: u8,
}

#[derive(AnchorSerialize, AnchorDeserialize)]
pub struct CookieResult {
    pub message_index: u16,
    pub total_calls: u32,
    pub calls_today: u8,
}

#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy)]
pub struct WithdrawTreasuryArgs {
    pub lamports: u64,
}
