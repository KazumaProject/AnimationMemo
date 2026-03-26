# Tree-sitter Dynamic Grammar Libraries

This folder is intentionally used for optional Tree-sitter grammar `.so` files.

## Why

The app already bundles AndroidIDE Tree-sitter grammars for:
- `kotlin`
- `java`
- `python`
- `json`
- `xml`
- `c`
- `cpp`

For additional languages (for example `javascript`, `typescript`, `tsx`, `html`, `css`, `bash`),
`TreeSitterBridge` tries to load grammars dynamically via:

- `TSLanguage.loadLanguage(context, grammarName)`

That expects native libraries named like:

- `libtree-sitter-javascript.so`
- `libtree-sitter-typescript.so`
- `libtree-sitter-tsx.so`
- `libtree-sitter-html.so`
- `libtree-sitter-css.so`
- `libtree-sitter-bash.so`

## Where to put files

Place ABI-specific files under standard Gradle locations, for example:

- `app/src/main/jniLibs/arm64-v8a/libtree-sitter-javascript.so`
- `app/src/main/jniLibs/arm64-v8a/libtree-sitter-typescript.so`

You can include additional ABIs (`armeabi-v7a`, `x86_64`) as needed.

## Notes

- If a dynamic grammar is missing, the app falls back to the strict lexer highlighter.
- No crash should occur for unsupported or missing grammar libraries.

