use anchor_lang::prelude::*;
use anchor_lang::system_program::{transfer, Transfer};
use anchor_lang::solana_program::system_instruction;
use anchor_lang::solana_program::{program::invoke_signed, system_program};
use solana_sha256_hasher::hashv;

use crate::constants::{
    CONFIG_SEED, DEFAULT_PENDING_ADMIN, MESSAGE_COUNT, TREASURY_VAULT_SEED, USER_SEED,
};
use crate::errors::OpenCookieError;

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

pub fn checked_add_u16(value: u16, delta: u16) -> Result<u16> {
    value
        .checked_add(delta)
        .ok_or(OpenCookieError::MathOverflow.into())
}

pub fn checked_add_u32(value: u32, delta: u32) -> Result<u32> {
    value
        .checked_add(delta)
        .ok_or(OpenCookieError::MathOverflow.into())
}

pub fn generate_message_index(
    slot: u64,
    unix_timestamp: i64,
    user: &Pubkey,
    total_calls: u32,
    calls_today: u16,
) -> u16 {
    let slot_bytes = slot.to_le_bytes();
    let ts_bytes = unix_timestamp.to_le_bytes();
    let total_bytes = total_calls.to_le_bytes();
    let calls_bytes = calls_today.to_le_bytes();

    let hash = hashv(&[
        &slot_bytes,
        &ts_bytes,
        user.as_ref(),
        &total_bytes,
        &calls_bytes,
    ]);

    let bytes = hash.to_bytes();
    let value = u64::from_le_bytes(bytes[0..8].try_into().unwrap());
    (value % MESSAGE_COUNT as u64) as u16
}

pub fn require_valid_system_account(account: &AccountInfo<'_>) -> Result<()> {
    require_keys_eq!(
        *account.owner,
        system_program::ID,
        OpenCookieError::InvalidSystemAccount
    );
    require!(account.data_is_empty(), OpenCookieError::InvalidSystemAccount);
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
        .ok_or(OpenCookieError::TreasuryWithdrawalTooLarge)?;
    require!(
        remaining_lamports >= minimum_balance,
        OpenCookieError::TreasuryWithdrawalTooLarge
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::constants::{DEFAULT_MAX_CALLS_PER_DAY, MESSAGE_COUNT};

    #[test]
    fn generate_message_index_is_always_below_message_count() {
        let user = Pubkey::new_unique();
        let samples: [(u64, i64, u32, u16); 8] = [
            (0, 0, 0, 0),
            (1, 1, 1, 1),
            (42, 1_700_000_000, 100, 100),
            (u64::MAX, i64::MAX, u32::MAX, u16::MAX),
            (123_456, -1, 999, 256),
            (999, 86_400, 500, 999),
            (7, 1_234_567_890, 0, 100),
            (888, 888, 888, 888),
        ];

        for (slot, ts, total, calls) in samples {
            let index = generate_message_index(slot, ts, &user, total, calls);
            assert!(
                index < MESSAGE_COUNT,
                "index {index} must be < {MESSAGE_COUNT} for sample ({slot}, {ts}, {total}, {calls})",
            );
        }
    }

    #[test]
    fn generate_message_index_changes_with_u16_calls_today() {
        let user = Pubkey::new_unique();
        let base = generate_message_index(100, 1_700_000_000, &user, 10, 255);
        let next = generate_message_index(100, 1_700_000_000, &user, 10, 256);
        assert_ne!(base, next);
    }

    #[test]
    fn checked_add_u16_rejects_overflow() {
        assert_eq!(checked_add_u16(1, 2).unwrap(), 3);
        assert!(checked_add_u16(u16::MAX, 1).is_err());
    }

    #[test]
    fn constants_match_product_requirements() {
        assert_eq!(MESSAGE_COUNT, 1000);
        assert_eq!(DEFAULT_MAX_CALLS_PER_DAY, 100);
    }

    #[test]
    fn current_day_id_uses_utc_days() {
        assert_eq!(current_day_id(0), 0);
        assert_eq!(current_day_id(86_399), 0);
        assert_eq!(current_day_id(86_400), 1);
    }
}
