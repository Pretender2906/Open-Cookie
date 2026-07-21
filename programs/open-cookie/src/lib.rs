use anchor_lang::prelude::*;

pub mod constants;
pub mod contexts;
pub mod errors;
pub mod helpers;
pub mod state;

use constants::{TREASURY_VAULT_SEED};
use contexts::*;
use errors::OpenCookieError;
use helpers::{
    charge_protocol_fee, checked_add_u16, checked_add_u32, current_day_id, derive_config_address,
    derive_treasury_vault_address, ensure_system_vault, generate_message_index, has_pending_admin,
    require_treasury_withdrawal_safe, require_valid_system_account, withdraw_from_treasury,
};
use state::{CookieResult, WithdrawTreasuryArgs};

declare_id!("CooknomesWJ3KdJUUYfgXBycS19hqDNS9riBavo2Gfuf");

#[program]
pub mod open_cookie {
    use super::*;

    pub fn initialize_config(
        ctx: Context<InitializeConfig>,
        pending_admin: Pubkey,
        price_lamports: u64,
        max_calls_per_day: u16,
    ) -> Result<()> {
        require!(max_calls_per_day > 0, OpenCookieError::InvalidConfig);
        let (expected_config, config_bump) = derive_config_address();
        require_keys_eq!(
            expected_config,
            ctx.accounts.config.key(),
            OpenCookieError::InvalidPda
        );

        let (expected_vault, treasury_bump) = derive_treasury_vault_address();
        require_keys_eq!(
            expected_vault,
            ctx.accounts.treasury_vault.key(),
            OpenCookieError::InvalidPda
        );

        let config = &mut ctx.accounts.config;
        config.admin_authority = ctx.accounts.admin.key();
        config.pending_admin = pending_admin;
        config.price_lamports = price_lamports;
        config.max_calls_per_day = max_calls_per_day;
        config.treasury_bump = treasury_bump;
        config.config_bump = config_bump;

        let treasury_bump_seed = [treasury_bump];
        ensure_system_vault(
            &ctx.accounts.admin,
            &ctx.accounts.treasury_vault.to_account_info(),
            &ctx.accounts.system_program,
            &[TREASURY_VAULT_SEED, &treasury_bump_seed],
        )?;

        Ok(())
    }

    pub fn update_config(
        ctx: Context<UpdateConfig>,
        pending_admin: Pubkey,
        price_lamports: u64,
        max_calls_per_day: u16,
    ) -> Result<()> {
        require!(max_calls_per_day > 0, OpenCookieError::InvalidConfig);
        require_keys_eq!(
            ctx.accounts.admin.key(),
            ctx.accounts.config.admin_authority,
            OpenCookieError::Unauthorized
        );

        let config = &mut ctx.accounts.config;
        config.pending_admin = pending_admin;
        config.price_lamports = price_lamports;
        config.max_calls_per_day = max_calls_per_day;
        Ok(())
    }

    pub fn accept_admin(ctx: Context<AcceptAdmin>) -> Result<()> {
        require!(
            has_pending_admin(&ctx.accounts.config),
            OpenCookieError::Unauthorized
        );
        require_keys_eq!(
            ctx.accounts.pending_admin.key(),
            ctx.accounts.config.pending_admin,
            OpenCookieError::Unauthorized
        );

        let config = &mut ctx.accounts.config;
        config.admin_authority = ctx.accounts.pending_admin.key();
        config.clear_pending_admin();
        Ok(())
    }

    pub fn initialize_user(ctx: Context<InitializeUser>) -> Result<()> {
        let clock = Clock::get()?;
        let profile = &mut ctx.accounts.user_profile;
        profile.owner = ctx.accounts.user.key();
        profile.total_calls = 0;
        profile.calls_today = 0;
        profile.last_day = current_day_id(clock.unix_timestamp);
        profile.bump = ctx.bumps.user_profile;
        Ok(())
    }

    pub fn close_user(_ctx: Context<CloseUser>) -> Result<()> {
        Ok(())
    }

    pub fn break_cookie(ctx: Context<BreakCookie>) -> Result<()> {
        let config = &ctx.accounts.config;
        let profile = &mut ctx.accounts.user_profile;
        let clock = Clock::get()?;

        let current_day = current_day_id(clock.unix_timestamp);
        if profile.last_day != current_day {
            profile.calls_today = 0;
            profile.last_day = current_day;
        }

        require!(
            profile.calls_today < config.max_calls_per_day,
            OpenCookieError::DailyLimitReached
        );

        require_valid_system_account(&ctx.accounts.treasury_vault.to_account_info())?;

        charge_protocol_fee(
            &ctx.accounts.user.to_account_info(),
            &ctx.accounts.treasury_vault.to_account_info(),
            &ctx.accounts.system_program,
            config.price_lamports,
        )?;

        profile.calls_today = checked_add_u16(profile.calls_today, 1)?;
        profile.total_calls = checked_add_u32(profile.total_calls, 1)?;

        let message_index = generate_message_index(
            clock.slot,
            clock.unix_timestamp,
            &ctx.accounts.user.key(),
            profile.total_calls,
            profile.calls_today,
        );

        let result = CookieResult {
            message_index,
            total_calls: profile.total_calls,
            calls_today: profile.calls_today,
        };

        anchor_lang::solana_program::program::set_return_data(&result.try_to_vec()?);
        Ok(())
    }

    pub fn withdraw_treasury(
        ctx: Context<WithdrawTreasury>,
        args: WithdrawTreasuryArgs,
    ) -> Result<()> {
        require_keys_eq!(
            ctx.accounts.admin.key(),
            ctx.accounts.config.admin_authority,
            OpenCookieError::Unauthorized
        );

        let (expected_vault, _) = derive_treasury_vault_address();
        require_keys_eq!(
            expected_vault,
            ctx.accounts.treasury_vault.key(),
            OpenCookieError::InvalidPda
        );

        require_valid_system_account(&ctx.accounts.treasury_vault.to_account_info())?;
        require_keys_neq!(
            ctx.accounts.destination.key(),
            ctx.accounts.treasury_vault.key(),
            OpenCookieError::InvalidPda
        );
        require_treasury_withdrawal_safe(
            &ctx.accounts.treasury_vault.to_account_info(),
            args.lamports,
        )?;

        let vault_bump = [ctx.accounts.config.treasury_bump];
        withdraw_from_treasury(
            &ctx.accounts.treasury_vault.to_account_info(),
            &ctx.accounts.destination.to_account_info(),
            &ctx.accounts.system_program,
            args.lamports,
            &[TREASURY_VAULT_SEED, &vault_bump],
        )?;

        Ok(())
    }
}
