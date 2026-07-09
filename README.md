# Fortune Button

Мінімальний Android dApp для [Solana Mobile dApp Store](https://solanamobile.com/) — щоденні ідеї після on-chain натискання кнопки Fortune.

> Це не гороскоп і не передбачення. Просто короткі щоденні підказки або маленькі завдання.

Репозиторій: [github.com/Pretender2906/Fortune-Button](https://github.com/Pretender2906/Fortune-Button)

## Архітектура

```
Android app (Compose + MWA 2.0)     Android admin (ops)
              ↓                              ↓
         fortune()                    update_config / accept_admin / withdraw_treasury
              ↓                              ↓
              Anchor Program (fortune_button)
                          ↓
                     Solana RPC
```

### On-chain

| PDA | Seeds | Призначення |
|-----|-------|-------------|
| Config | `["config"]` | admin_authority, pending_admin, price |
| Treasury vault | `["treasury-vault"]` | system-owned PDA для fee |
| UserProfile | `["user", owner]` | total_calls, calls_today, last_day |

**Інструкції:** `initialize_config`, `update_config`, `accept_admin`, `initialize_user`, `close_user`, `fortune`, `withdraw_treasury`

**Admin flow (як у Nudge):**
1. Deployer викликає `initialize_config` (потрібен program upgrade authority)
2. Admin може призначити `pending_admin` через `update_config`
3. Новий admin підтверджує через `accept_admin`
4. Fee з `fortune()` збираються на `treasury-vault` PDA
5. Admin виводить через `withdraw_treasury`

**`fortune`** — головна інструкція:
1. Скидає лічильник на новий день
2. Перевіряє ліміт 3/день
3. Переводить fee на treasury-vault PDA (якщо price > 0)
4. Генерує `fortune_index` через `hashv(...) % 200`
5. Повертає `FortuneResult` через `set_return_data`

### Android

| Модуль | Package | Призначення |
|--------|---------|-------------|
| `:app` | `com.fortunebutton.app` | Користувацький dApp — кнопка Fortune |
| `:admin` | `com.fortunebutton.admin` | Ops — config, admin transfer, treasury withdraw |

Тексти fortunes зберігаються локально в `android/app/src/main/assets/`.

## Швидкий старт

### Anchor program

```bash
anchor keys list
anchor build
npm install
anchor test
```

### Devnet deploy

```bash
anchor deploy --provider.cluster devnet

# pending_admin = zero pubkey, price = 10000 lamports
node scripts/init-config-devnet.mjs 10000

# Ops
node scripts/pda-derive.mjs
node scripts/update-config-devnet.mjs <NEW_ADMIN_PUBKEY> 15000
node scripts/accept-admin-devnet.mjs
node scripts/withdraw-treasury-devnet.mjs status
node scripts/withdraw-treasury-devnet.mjs withdraw <DESTINATION> 5000
```

### Android

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :admin:assembleDebug
```

## Program ID (placeholder)

```
FrtnBtnPK86hRM2pMF7FesE38MYDi59z9dMuNyfxiq
```

Після `anchor keys sync` оновіть ID у `lib.rs`, `Anchor.toml`, `ProgramAddresses.kt` (app + admin).

## Ліцензія

MIT
