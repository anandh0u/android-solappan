# Third-party notices

SOL bundles Vosk Android 0.3.75 and the Vosk small US English 0.15 speech model by Alpha Cephei. Both are Apache-2.0 licensed. Model source: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip

The bundled model archive SHA-256 is `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`.

The unmodified archive is bundled for offline startup without a separate phone download. Recognition runs locally; the model is not an LLM. Apache license text is packaged in `app/src/main/assets/VOSK-LICENSE.txt`. Vosk source and copyright: https://github.com/alphacep/vosk-api (Copyright Alpha Cephei Inc.).

Vosk depends on Java Native Access (JNA), distributed under LGPL-2.1-or-later / Apache-2.0 dual licensing. SOL uses its Apache-2.0 option. See https://github.com/java-native-access/jna/blob/master/LICENSE.

AndroidX, Kotlin and other build/runtime dependencies retain their respective licenses. A complete release dependency/SBOM review is tracked in the release qualification issue.
