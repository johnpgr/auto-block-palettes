# Tech Stack

## Runtime Environment

| Component | Choice | Version |
|-----------|--------|---------|
| Game | Minecraft | 1.21.11 |
| Java | OpenJDK | 21 |

## Mod Loaders

This project uses the MultiLoader architecture to support all major Minecraft mod loaders from a single codebase:

| Loader | Version | Notes |
|--------|---------|-------|
| Fabric | 0.139.5+1.21.11 | Primary development target |
| Fabric Loader | 0.18.2 | |
| Forge | 61.0.1 | Legacy loader support |
| NeoForge | 21.11.3-beta | Modern Forge successor |

## Build System

| Component | Choice | Notes |
|-----------|--------|-------|
| Build Tool | Gradle | Groovy DSL |
| Java Toolchain | 21 | Enforced via Gradle |
| Mapping Provider | ParchmentMC | Human-readable parameter names |
| NeoForm | 1.21.11-20251209.172050 | Decompilation toolchain |

## Core Libraries

| Library | Purpose | Notes |
|---------|---------|-------|
| Mixin | Bytecode modification | Cross-loader injection for intercepting game events |
| SLF4J | Logging | Standard logging facade, provided by Minecraft |

## Architecture

### Module Structure

```
auto-block-palettes/
├── common/          # Shared code (vanilla MC API only)
├── fabric/          # Fabric-specific entry points and implementations
├── forge/           # Forge-specific entry points and implementations
├── neoforge/        # NeoForge-specific entry points and implementations
└── buildSrc/        # Gradle build configuration
```

### Design Patterns

| Pattern | Usage |
|---------|-------|
| Service Loader | Platform abstraction between common and loader-specific code |
| Mixin Injection | Intercepting block placement and inventory events |

## Client-Side Constraints

This mod is **client-side only**:

- No server-side code or networking required
- Works on vanilla servers without server installation
- All state (palettes, settings) stored locally on the client
- No permissions or server compatibility concerns

## Data Storage

| Data Type | Format | Location |
|-----------|--------|----------|
| Palette Definitions | JSON | Client config directory |
| Mod Settings | JSON | Client config directory |

## GUI Framework

| Component | Approach |
|-----------|----------|
| Screens | Vanilla Minecraft Screen API |
| Widgets | Vanilla Button, EditBox, and custom widgets |
| Rendering | Vanilla GuiGraphics API |

## Development Tools

| Tool | Purpose |
|------|---------|
| IntelliJ IDEA | Recommended IDE |
| Gradle Wrapper | Consistent build environment |
| mise | Runtime version management |
