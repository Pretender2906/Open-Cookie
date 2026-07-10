use anchor_lang::prelude::*;

use crate::constants::{CONFIG_LEN, CONFIG_SEED, TREASURY_VAULT_SEED, USER_PROFILE_LEN, USER_SEED};
use crate::state::{Config, UserProfile};

#[derive(Accounts)]
pub struct InitializeConfig<'info> {
    #[account(mut)]
    pub admin: Signer<'info>,

    #[account(
        init,
        payer = admin,
        space = CONFIG_LEN,
        seeds = [CONFIG_SEED],
        bump,
    )]
    pub config: Account<'info, Config>,

    /// CHECK: Created as a system-owned PDA in the instruction handler.
    #[account(mut, seeds = [TREASURY_VAULT_SEED], bump)]
    pub treasury_vault: UncheckedAccount<'info>,

    /// Binds `program_data` to this program so it cannot be swapped for another
    /// upgradeable program's ProgramData account.
    #[account(
        constraint = program.programdata_address()? == Some(program_data.key())
            @ crate::errors::OpenCookieError::InvalidPda
    )]
    pub program: Program<'info, crate::program::OpenCookie>,

    /// Ensures only the program upgrade authority can initialize config.
    #[account(
        constraint = program_data.upgrade_authority_address == Some(admin.key())
            @ crate::errors::OpenCookieError::Unauthorized
    )]
    pub program_data: Account<'info, ProgramData>,

    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct UpdateConfig<'info> {
    pub admin: Signer<'info>,

    #[account(
        mut,
        seeds = [CONFIG_SEED],
        bump = config.config_bump,
    )]
    pub config: Account<'info, Config>,
}

#[derive(Accounts)]
pub struct AcceptAdmin<'info> {
    pub pending_admin: Signer<'info>,

    #[account(
        mut,
        seeds = [CONFIG_SEED],
        bump = config.config_bump,
    )]
    pub config: Account<'info, Config>,
}

#[derive(Accounts)]
pub struct InitializeUser<'info> {
    #[account(mut)]
    pub user: Signer<'info>,

    #[account(seeds = [CONFIG_SEED], bump = config.config_bump)]
    pub config: Account<'info, Config>,

    #[account(
        init,
        payer = user,
        space = USER_PROFILE_LEN,
        seeds = [USER_SEED, user.key().as_ref()],
        bump,
    )]
    pub user_profile: Account<'info, UserProfile>,

    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct CloseUser<'info> {
    #[account(mut)]
    pub user: Signer<'info>,

    #[account(seeds = [CONFIG_SEED], bump = config.config_bump)]
    pub config: Account<'info, Config>,

    #[account(
        mut,
        close = user,
        seeds = [USER_SEED, user.key().as_ref()],
        bump = user_profile.bump,
        constraint = user_profile.owner == user.key() @ crate::errors::OpenCookieError::Unauthorized,
    )]
    pub user_profile: Account<'info, UserProfile>,
}

#[derive(Accounts)]
pub struct BreakCookie<'info> {
    #[account(mut)]
    pub user: Signer<'info>,

    #[account(seeds = [CONFIG_SEED], bump = config.config_bump)]
    pub config: Account<'info, Config>,

    #[account(
        mut,
        seeds = [USER_SEED, user.key().as_ref()],
        bump = user_profile.bump,
        constraint = user_profile.owner == user.key() @ crate::errors::OpenCookieError::Unauthorized,
    )]
    pub user_profile: Account<'info, UserProfile>,

    /// CHECK: System-owned PDA vault validated by seeds and bump.
    #[account(mut, seeds = [TREASURY_VAULT_SEED], bump = config.treasury_bump)]
    pub treasury_vault: UncheckedAccount<'info>,

    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct WithdrawTreasury<'info> {
    pub admin: Signer<'info>,

    #[account(seeds = [CONFIG_SEED], bump = config.config_bump)]
    pub config: Account<'info, Config>,

    /// CHECK: System-owned PDA vault validated by seeds and bump.
    #[account(mut, seeds = [TREASURY_VAULT_SEED], bump = config.treasury_bump)]
    pub treasury_vault: UncheckedAccount<'info>,

    /// CHECK: Destination chosen by admin policy.
    #[account(mut)]
    pub destination: UncheckedAccount<'info>,

    pub system_program: Program<'info, System>,
}
