# Third-party notices

Flux Streaming is licensed under the GNU General Public License version 3. These parts come from
other projects and keep their own licenses.

## Concentus

- Project: Concentus, a pure Java port of the Opus audio codec (`io.github.jaredmdobson:concentus`)
- Source repository: https://github.com/lostromb/concentus
- License: BSD 3-Clause (as libopus), used unmodified as a library dependency

Used to encode the phone's sound as Opus when streaming to Moonlight.

## pyatv

- Project: pyatv, a Python client for Apple TV and AirPlay
- Source repository: https://github.com/postlund/pyatv
- License: MIT
- License text: `third_party/licenses/pyatv-MIT.txt`

The AirPlay client in `app/src/main/java/it/palsoftware/pastiera/airplay/` follows pyatv's AirPlay implementation: HomeKit pairing (transient and PIN), the encrypted control and event channels, and playing a URL. Written anew in Kotlin; no pyatv code is included.

## Bouncy Castle

- Project: The Legion of the Bouncy Castle Java cryptography APIs (`org.bouncycastle:bcprov-jdk15to18`)
- Source repository: https://github.com/bcgit/bc-java
- License: MIT-style Bouncy Castle licence, used unmodified as a library dependency

Used for AirPlay pairing: X25519, Ed25519, ChaCha20-Poly1305 and HKDF.
