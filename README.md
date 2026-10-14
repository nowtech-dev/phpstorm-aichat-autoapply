# AI Chat: Auto Always Allow

JetBrains IDE plugin that automatically approves sensitive actions in **AI Chat** (terminal runs, MCP/ACP agent tool permissions such as **Cursor**), by choosing **Allow always** when the chat UI offers it.

**Plugin ID:** `dev.nowtech.aichat.autoapply`  
**Vendor:** [NOW Tech](https://nowtech.dev)

## Requirements

- PhpStorm / IntelliJ **2026.2+** (build `262.10968+`)
- Bundled or installed plugin **AI Assistant** (`com.intellij.ml.llm`)

## Features

- Auto-responds to permission prompts with **Allow always** (falls back to **Allow once** if needed)
- Optional filter: terminal + agent permission prompts only (default **on**)
- **Tools → AI Chat Auto Allow: On/Off** toggle
- **Settings → Tools → AI Chat Auto Allow** for the same options
- Enabled by default on first install

## Build

1. Copy local paths:

   ```bash
   cp gradle.properties.example gradle.properties
   ```

   Edit `gradle.properties`:

   - `phpstorm.home` — your IDE install directory
   - `ml.llm.lib` — `ml-llm/lib` inside the AI Assistant plugin (e.g. `%APPDATA%\JetBrains\PhpStorm2026.2\plugins\ml-llm\lib` on Windows)

2. Build the distribution ZIP:

   ```bash
   ./builder build
   ```

   Output: `build/distributions/aichat-auto-apply-*.zip`

## Install

1. **Settings → Plugins → ⚙ → Install Plugin from Disk…** → select the ZIP  
2. **Restart the IDE** (required; dynamic load is not supported)

## Verify

After opening a project, check the IDE log for:

```text
AI Chat auto-allow: bootstrap ok
AI Chat auto-allow: watching project "..."
```

Logger id: `dev.nowtech.aichat.autoapply`

## Disclaimer

This plugin uses **internal** APIs of JetBrains AI Assistant. It may break on IDE or AI Assistant updates. Not affiliated with JetBrains or Cursor.

## License

Copyright © NOW Tech. All rights reserved unless a license file is added separately.
