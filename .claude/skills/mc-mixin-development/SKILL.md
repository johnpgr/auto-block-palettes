---
name: Minecraft Mixin Development
description: Write SpongePowered Mixin code to modify Minecraft's bytecode safely and effectively. Use this skill when you need to modify vanilla Minecraft behavior that cannot be achieved through normal APIs. Apply when creating or editing files in mixin/ packages or *mixins.json configuration files. Essential when using @Inject, @Redirect, @ModifyVariable, @ModifyArg, or other mixin annotations. Use when troubleshooting mixin conflicts, choosing injection points, or handling CallbackInfo/CallbackInfoReturnable. Critical for understanding mixin compatibility and performance implications.
---

## Standards Reference

For detailed standards, refer to: [Mixin Development Standards](../../../agent-os/standards/development/mixins.md)

## When to use this skill:

- When creating new mixin classes to modify vanilla Minecraft behavior
- When editing existing mixin classes or adding new injection points
- When configuring `*mixins.json` files
- When choosing between @Inject, @Redirect, @ModifyVariable, @ModifyArg, @Overwrite
- When working with CallbackInfo and CallbackInfoReturnable
- When targeting specific injection points with @At
- When debugging mixin application failures or conflicts
- When accessing private fields/methods in target classes with @Shadow or @Accessor
- When modifying constructor behavior with `<init>` targets

## Mixin Basics

Mixins allow bytecode modification of Minecraft classes at runtime:

```java
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Runs at the end of Minecraft's constructor
        Constants.LOG.info("Minecraft initialized!");
    }
}
```

## Common Mixin Annotations

### @Inject - Add code at specific points

```java
// Inject at method start
@Inject(method = "tick", at = @At("HEAD"))
private void onTickStart(CallbackInfo ci) { }

// Inject at method end
@Inject(method = "tick", at = @At("TAIL"))
private void onTickEnd(CallbackInfo ci) { }

// Inject before specific method call
@Inject(method = "render", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(...)V"))
private void beforeDrawString(CallbackInfo ci) { }

// Inject and cancel (return early)
@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
private void onAttack(Entity target, CallbackInfo ci) {
    if (shouldCancel(target)) {
        ci.cancel(); // Prevents original method from running
    }
}

// Inject into method with return value
@Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
private void modifyHealth(CallbackInfoReturnable<Float> cir) {
    cir.setReturnValue(cir.getReturnValue() * 2); // Double the health
}
```

### @Shadow - Access private members

```java
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    private float health;  // Access private field

    @Shadow
    protected abstract void doSomething();  // Access protected method

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.health += 1;  // Now we can use it
        this.doSomething();
    }
}
```

### @Redirect - Replace method calls

```java
@Mixin(SomeClass.class)
public class MixinSomeClass {

    // Replace a specific method call within the target method
    @Redirect(method = "process", at = @At(value = "INVOKE",
        target = "Ljava/util/List;size()I"))
    private int redirectListSize(List<?> list) {
        return list.size() * 2;  // Return modified value
    }
}
```

### @ModifyVariable - Change local variables

```java
@Mixin(SomeClass.class)
public class MixinSomeClass {

    @ModifyVariable(method = "calculate", at = @At("STORE"), ordinal = 0)
    private int modifyResult(int original) {
        return original + 10;
    }
}
```

### @ModifyArg - Change method arguments

```java
@Mixin(SomeClass.class)
public class MixinSomeClass {

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
        target = "Lsomething;draw(IIII)V"), index = 2)
    private int modifyWidth(int width) {
        return width * 2;
    }
}
```

### @Accessor - Generate getters/setters

```java
@Mixin(SomeClass.class)
public interface SomeClassAccessor {

    @Accessor("privateField")
    int getPrivateField();

    @Accessor("privateField")
    void setPrivateField(int value);
}

// Usage: ((SomeClassAccessor) instance).getPrivateField()
```

## Injection Point Reference (@At values)

| Value | Description |
|-------|-------------|
| `HEAD` | Start of method |
| `TAIL` | End of method (before return) |
| `RETURN` | At each return statement |
| `INVOKE` | Before/after method call |
| `INVOKE_ASSIGN` | After method call that stores result |
| `FIELD` | Field access (GET/PUT) |
| `NEW` | Object instantiation |
| `CONSTANT` | Constant value usage |

## Mixin Configuration (mixins.json)

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.example.mod.mixin",
  "refmap": "${mod_id}.refmap.json",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "MixinServerLevel",
    "MixinBlockEntity"
  ],
  "client": [
    "MixinMinecraft",
    "MixinGameRenderer",
    "MixinScreen"
  ],
  "server": [
    "MixinDedicatedServer"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

**Important:** Put client-targeting mixins in `"client"` array, not `"mixins"`!

## Best Practices

### 1. Use the least invasive option
```
Prefer:  @Inject > @Redirect > @ModifyVariable > @Overwrite
```
`@Overwrite` replaces the entire method and breaks compatibility with other mods.

### 2. Be specific with injection points
```java
// BAD - Might hit wrong call
@At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z")

// GOOD - Use ordinal for specific occurrence
@At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 2)

// GOOD - Use shift to fine-tune position
@At(value = "INVOKE", target = "...", shift = At.Shift.AFTER)
```

### 3. Handle method signatures correctly
```java
// Method: public void process(String name, int count)
// Target: "process(Ljava/lang/String;I)V"

// Method: private static List<Item> getItems()
// Target: "getItems()Ljava/util/List;"
```

### 4. Match parameter order in callbacks
```java
// Original: public void attack(Entity target, float damage)
@Inject(method = "attack(Lnet/minecraft/entity/Entity;F)V", at = @At("HEAD"))
private void onAttack(Entity target, float damage, CallbackInfo ci) {
    // Parameters match original, CallbackInfo last
}
```

## Common Issues

### Mixin not applying
- Check package path in mixins.json
- Verify class name is listed in correct array (mixins/client/server)
- Check for typos in method targets
- Run with `-Dmixin.debug=true` for detailed output

### Method not found
- Check Minecraft mappings (Mojmap vs Yarn vs SRG)
- Verify method signature matches exactly
- Some methods are different between client/server

### Conflict with other mods
- Avoid @Overwrite when possible
- Use high/low priority: `@Mixin(value = Target.class, priority = 1001)`
- Document your mixin targets for other developers

## Mapping Considerations

This project uses **Mojang mappings** (official). Method names and signatures must match.

For obfuscated targets, use `@At(target = "...")` with correct mapping:
```java
// Mojmap: "tick()V"
// Yarn: "method_5773()V" (different in Fabric Yarn)
// SRG: "func_70071_h_()V" (Forge SRG)
```
