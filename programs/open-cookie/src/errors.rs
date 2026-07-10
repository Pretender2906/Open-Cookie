use anchor_lang::prelude::*;

#[error_code]
pub enum OpenCookieError {
    #[msg("Config already initialized")]
    ConfigAlreadyInitialized,
    #[msg("Config not initialized")]
    ConfigNotInitialized,
    #[msg("Unauthorized")]
    Unauthorized,
    #[msg("User profile already initialized")]
    UserAlreadyInitialized,
    #[msg("User profile not initialized")]
    UserNotInitialized,
    #[msg("Daily limit reached")]
    DailyLimitReached,
    #[msg("Invalid PDA")]
    InvalidPda,
    #[msg("Math overflow")]
    MathOverflow,
    #[msg("Invalid system account")]
    InvalidSystemAccount,
    #[msg("Treasury withdrawal too large")]
    TreasuryWithdrawalTooLarge,
}
