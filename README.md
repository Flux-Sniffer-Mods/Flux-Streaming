# Flux Streaming

**Your phone as a Moonlight host.** Moonlight on a TV, a PC or another phone finds it on the Wi-Fi
and pairs with a PIN, then lists your games with their art, the phone's screen, Android's desktop
as a screen of its own for any app, your apps and Termux scripts. It also casts the phone's screen
to AirPlay and Miracast TVs. No root needed: it runs through its built-in shell, paired once with
wireless debugging (or another Flux app's).

Part of the Flux apps: [Flux Keyboard](https://github.com/Flux-Sniffer-Mods/Flux-Keyboard),
[Flux Gaming](https://github.com/Flux-Sniffer-Mods/Flux-Gaming), Flux Streaming and
[Flux Desktop](https://github.com/Flux-Sniffer-Mods/Flux-Desktop). Each is an app of its own;
installed together they work as one: your games and their art come from Flux Gaming, which plays
them with the keys while they stream, and the Linux desktop from Flux Desktop.

## What it does

- **Moonlight host**: pairs with a PIN, streams at the client's resolution with the phone's sound, its controller, keyboard and mouse passed on.
- **A screen of its own**: a game or Android's desktop on the TV while the phone stays free, or the phone's screen mirrored.
- **The TV**: turned on and Moonlight opened straight into a game, its volume on the phone's keys.
- **AirPlay and Miracast**: the phone's screen on an Apple TV or a Miracast TV, no app there.
- **Quick Settings tile** for streaming, and "Always available" so Moonlight can always start it.

## Get it

Download the APK from [Releases](https://github.com/Flux-Sniffer-Mods/Flux-Streaming/releases). It
updates itself from there.

## Building

```
git clone --recurse-submodules https://github.com/Flux-Sniffer-Mods/Flux-Streaming
./gradlew :app:assembleDebug
```

`core/` is [Flux Core](https://github.com/Flux-Sniffer-Mods/Flux-Core), what the Flux apps share.

## License

GPL-3.0. Third-party parts: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). What changed:
[CHANGES.md](CHANGES.md).
