# RP Core — что менялось

## 0.15.0 (16.11.2026) — общие механизмы для модов семейства

Только добавления: прежний API не менялся, старые jar модов работают с этой версией.

- `api/data/PlayerStore`, `api/data/PersistentTag` — данные игрока с общим жизненным циклом (сохранение, смерть,
  возврат из Края, отправка клиенту, выход).
- `api/config/SettingsBinder` — привязка полей класса настроек к конфигу.
- `api/Worn` — надетое в слотах Curios (Curios необязателен).
- `RpCoreAPI.BODY_REFERENCE`, `RpCoreAPI.bodyRate()` — один эталон времени тела.
- `api/data/Matcher` — список id и тегов из датапака.
- Розетки: `BodyProvider.down/withdrawal/craving/formerAddiction/antipsychotic/infect`, `MindProvider`
  (`RpCoreAPI.registerMind`, `stressImpulse`, `sanityHit`), `TraitProvider.teach`
  (`RpCoreAPI.grantKnowledge`), `RpCoreAPI.grantAdvancement`.
- `api/LightProvider` (`RpCoreAPI.registerLight`, `carriesLight`) — «несёт включённый свет».
- `RpCoreAPI.reportCompat`, `compat()`, команда `/rpcore compat` — отчёт о необязательных интеграциях модов семейства.
- `Worn.find` — первый надетый предмет без создания списков.
