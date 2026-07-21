# Open Cookie

Мінімальний Android dApp для [Solana Mobile dApp Store](https://solanamobile.com/) — щоденні повідомлення після on-chain «розколу печива».

> Це не гороскоп і не передбачення. Просто печиво з короткою думкою, порадою або маленьким завданням на день.

Репозиторій: [github.com/Pretender2906/Open-Cookie](https://github.com/Pretender2906/Open-Cookie)

## Архітектура

```
Android app (Compose + MWA 2.0)     Android admin (ops)
              ↓                              ↓
         break_cookie()              update_config / accept_admin / withdraw_treasury
              ↓                              ↓
              Anchor Program (open_cookie)
                          ↓
                     Solana RPC
```

### On-chain

| PDA | Seeds | Призначення |
|-----|-------|-------------|
| Config | `["config"]` | admin_authority, pending_admin, price |
| Treasury vault | `["treasury-vault"]` | system-owned PDA для fee |
| UserProfile | `["user", owner]` | total_calls, calls_today, last_day |

**Інструкції:** `initialize_config`, `update_config`, `accept_admin`, `initialize_user`, `close_user`, `break_cookie`, `withdraw_treasury`

**Admin flow (як у Nudge):**
1. Deployer викликає `initialize_config` (потрібен program upgrade authority)
2. Admin може призначити `pending_admin` через `update_config`
3. Новий admin підтверджує через `accept_admin`
4. Fee з `break_cookie()` збираються на `treasury-vault` PDA
5. Admin виводить через `withdraw_treasury`

**`break_cookie`** — головна інструкція:
1. Скидає лічильник на новий день
2. Перевіряє ліміт 3/день
3. Переводить fee на treasury-vault PDA (якщо price > 0)
4. Генерує `message_index` через `hashv(...) % 200`
5. Повертає `CookieResult` через `set_return_data`

### Android

| Модуль | Package | Призначення |
|--------|---------|-------------|
| `:app` | `com.opencookie.app` | Користувацький dApp — Break Cookie |
| `:admin` | `com.opencookie.admin` | Ops — config, admin transfer, treasury withdraw |

Тексти повідомлень зберігаються локально в `android/app/src/main/assets/messages_*.json`.

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

## Program ID

```
CooknomesWJ3KdJUUYfgXBycS19hqDNS9riBavo2Gfuf
```

### Синхронізація keypair (після `solana-keygen grind`)

Anchor очікує keypair у `target/deploy/open_cookie-keypair.json` (ім'я crate з `Cargo.toml`).

```bash
mkdir -p target/deploy
cp /path/to/CooknomesWJ3KdJUUYfgXBycS19hqDNS9riBavo2Gfuf.json target/deploy/open_cookie-keypair.json

anchor keys list    # перевірити, що open_cookie = Cookn...
anchor keys sync    # оновить lib.rs + Anchor.toml (якщо щось розійшлось)
anchor build
```

Після зміни Program ID також оновіть:
- `programs/open-cookie/src/lib.rs` (`declare_id!`)
- `Anchor.toml`
- `scripts/cluster-common.mjs`
- `ProgramAddresses.kt` (app + admin)

## Ліцензія

MIT
