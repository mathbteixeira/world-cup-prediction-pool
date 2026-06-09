UPDATE user_accounts
SET role = 'ADMIN'
WHERE lower(email) = lower('mbateixeira@yahoo.com.br');