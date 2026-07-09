use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};
use solana_program::hash::hashv;
use solana_program::system_instruction;
use solana_program::{program::invoke_signed, system_program};

use crate::constants::{
    CONFIG_SEED, DEFAULT_PENDING_ADMIN, FORTUNE_COUNT, TREASURY_VAULT_SEED, USER_SEED,
};
use crate::errors::FortuneError;

pub fn derive_config_address() -> (Pubkey, u8) {
    Pubkey::find_program_address(&[CONFIG_SEED], &crate::ID)
}

pub fn derive_user_address(owner: &Pubkey) -> (Pubkey, u8) {
    Pubkey::find_program_address(&[USER_SEED, owner.as_ref()], &crate::ID)
}

pub fn derive_treasury_vault_address() -> (Pubkey, u8) {
    Pubkey::find_program_address(&[TREASURY_VAULT_SEED], &crate::ID)
}

pub fn has_pending_admin(config: &crate::state::Config) -> bool {
    config.pending_admin != DEFAULT_PENDING_ADMIN
}

pub fn current_day_id(unix_timestamp: i64) -> i32 {
    (unix_timestamp / 86_400) as i32
}

pub fn checked_add_u8(value: u8, delta: u8) -> Result<u8> {
    value
        .checked_add(delta)
        .ok_or(FortuneError::MathOverflow.into())
}

pub fn checked_add_u32(value: u32, delta: u32) -> Result<u32> {
    value
        .checked_add(delta)
        .ok_or(FortuneError::MathOverflow.into())
}

pub fn generate_fortune_index(
    slot: u64,
    unix_timestamp: i64,
    user: &Pubkey,
    total_calls: u32,
    calls_today: u8,
) -> u16 {
    let slot_bytes = slot.to_le_bytes();
    let ts_bytes = unix_timestamp.to_le_bytes();
    let total_bytes = total_calls.to_le_bytes();
    let calls_bytes = [calls_today];

    let hash = hashv(&[
        &slot_bytes,
        &ts_bytes,
        user.as_ref(),
        &total_bytes,
        &calls_bytes,
    ]);

    let bytes = hash.to_bytes();
    let value = u64::from_le_bytes(bytes[0..8].try_into().unwrap());
    (value % FORTUNE_COUNT as u64) as u16
}

pub fn require_valid_system_account(account: &AccountInfo<'_>) -> Result<()> {
    require_keys_eq!(
        *account.owner,
        system_program::ID,
        FortuneError::InvalidSystemAccount
    );
    require!(account.data_is_empty(), FortuneError::InvalidSystemAccount);
    Ok(())
}

fn is_missing_account(account: &AccountInfo<'_>) -> bool {
    account.lamports() == 0 && account.owner == &system_program::ID && account.data_is_empty()
}

pub fn ensure_system_vault<'info>(
    payer: &Signer<'info>,
    treasury_vault: &AccountInfo<'info>,
    system_program_account: &Program<'info, System>,
    signer_seeds: &[&[u8]],
) -> Result<()> {
    if is_missing_account(treasury_vault) {
        let create_ix = system_instruction::create_account(
            &payer.key(),
            treasury_vault.key,
            Rent::get()?.minimum_balance(0),
            0,
            &system_program::ID,
        );

        invoke_signed(
            &create_ix,
            &[
                payer.to_account_info(),
                treasury_vault.clone(),
                system_program_account.to_account_info(),
            ],
            &[signer_seeds],
        )?;
    }

    require_valid_system_account(treasury_vault)?;
    Ok(())
}

pub fn charge_protocol_fee<'info>(
    payer: &AccountInfo<'info>,
    treasury_vault: &AccountInfo<'info>,
    system_program_account: &Program<'info, System>,
    lamports: u64,
) -> Result<()> {
    if lamports == 0 {
        return Ok(());
    }

    transfer(
        CpiContext::new(
            system_program_account.to_account_info(),
            Transfer {
                from: payer.clone(),
                to: treasury_vault.clone(),
            },
        ),
        lamports,
    )
}

pub fn require_treasury_withdrawal_safe(
    treasury_vault: &AccountInfo<'_>,
    lamports: u64,
) -> Result<()> {
    let minimum_balance = Rent::get()?.minimum_balance(0);
    let remaining_lamports = treasury_vault
        .lamports()
        .checked_sub(lamports)
        .ok_or(FortuneError::TreasuryWithdrawalTooLarge)?;
    require!(
        remaining_lamports >= minimum_balance,
        FortuneError::TreasuryWithdrawalTooLarge
    );
    Ok(())
}

pub fn withdraw_from_treasury<'info>(
    treasury_vault: &AccountInfo<'info>,
    destination: &AccountInfo<'info>,
    system_program_account: &Program<'info, System>,
    lamports: u64,
    signer_seeds: &[&[u8]],
) -> Result<()> {
    if lamports == 0 {
        return Ok(());
    }

    transfer(
        CpiContext::new_with_signer(
            system_program_account.to_account_info(),
            Transfer {
                from: treasury_vault.clone(),
                to: destination.clone(),
            },
            &[signer_seeds],
        ),
        lamports,
    )
}
