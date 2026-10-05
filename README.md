# kekkai android

Architecture:

- **Decrypt**: OpenKeychain's Remote OpenPGP API via `openpgp-ktx`
  (background, non-interactive after a one-time grant) — no bundled GPG/pinentry.
- **Sync**: in-app git (JGit), so the app doesn't require Termux.
- **Interaction**: `AutofillService`-backed, manual search only — no automatic
  package/domain matching heuristics. Every autofillable field offers one
  generic "search kekkai" suggestion; picking an entry fills every field
  matched by standard Autofill hint, plus always the field that was actually
  focused. Clipboard copy (marked sensitive, auto-cleared) is the fallback for
  when the app is opened standalone instead of through Autofill.

This mirrors sibling project **kekkai-desktop**'s shape: a thin, platform-
native connector against the same repository contract (its
`internal/repository`), not a shared runtime or RPC backend — git already
handles distributing the store between devices, so no daemon or network
protocol is needed between frontends.

## Status

Builds and passes `compileDebugKotlin`/`lint`/`assembleDebug`. Known gap: no
onboarding/settings UI yet — the git remote URL is read from an undocumented
SharedPreferences key and sync is silently skipped if unset. Not yet tested
against a real device, OpenKeychain install, or pass store.

## License

GPLv3 — see [LICENSE](LICENSE).
