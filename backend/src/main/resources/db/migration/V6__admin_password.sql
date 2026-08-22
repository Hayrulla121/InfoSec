-- Flyway migration V6: replace the seeded administrator password.
--
-- V2 seeded 'admin' with the password 'admin' - fine for the first week of
-- development, indefensible for a bank. This sets a long random one instead.
--
-- WHY A NEW FILE RATHER THAN AN EDIT TO V2. Flyway records a checksum of every
-- migration it has run. Editing V2 in place would change that checksum, and
-- every database where V2 had already run - including every developer's
-- ./data/riskdb - would refuse to start with a validation error. V2 stays
-- exactly as it was; this file corrects it afterwards. A fresh database runs
-- V2 then V6 and lands in the same state as an existing one.
--
-- The value is a BCrypt hash at cost factor 10, matching the encoder Spring is
-- configured with. BCrypt carries its own salt inside the string, so this is
-- not reversible and cannot be looked up. The $2y$ prefix is one of the three
-- BCrypt revisions Spring's BCryptPasswordEncoder accepts ($2a$/$2y$/$2b$);
-- they differ only in a header byte, not in the algorithm.
--
-- The plaintext is recorded in README.md, because a credential nobody can find
-- is a credential someone will reset in a panic at 3am. That does mean anyone
-- holding this repository holds the password: THIS IS STILL A DEVELOPMENT
-- DEFAULT. Before the system carries real data, change it from Пользователи /
-- Foydalanuvchilar in the interface, which re-hashes through the same encoder.
UPDATE users
SET password_hash = '$2y$10$bnSQjr.2IDZ9x0/iqiyQOuTLqDOxixf4OXwLhk.pHM0x.VoUeejoG',
    updated_at    = CURRENT_TIMESTAMP
WHERE username = 'admin';
