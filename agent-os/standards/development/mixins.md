# Mixin Development Standards

This document defines standards for writing SpongePowered Mixins in Minecraft mods.

## When to Use Mixins

Use mixins when:
- No event/hook exists for your use case
- You need to modify private/internal behavior
- You need to inject into constructors
- You need to change control flow

**Prefer events/hooks when available** - they're more compatible with other mods.

## Annotation Priority

Use the least invasive option:

```
@Inject > @Redirect > @ModifyVariable > @ModifyArg > @Overwrite
```

`@Overwrite` replaces entire methods and breaks compatibility. Use only as last resort.

## Mixin Class Structure

```java
@Mixin(TargetClass.class)
public abstract class MixinTargetClass {

    // 1. @Shadow fields/methods first
    @Shadow
    private int someField;

    @Shadow
    protected abstract void someMethod();

    // 2. @Inject methods
    @Inject(method = "targetMethod", at = @At("HEAD"))
    private void modid$onMethodHead(CallbackInfo ci) {
        // Implementation
    }

    // 3. Other modifications (@Redirect, @ModifyVariable, etc.)
}
```

## Naming Conventions

### Mixin Classes
- Prefix with `Mixin`: `MixinMinecraft`, `MixinEntity`
- Match target class name when possible

### Injected Methods
- Prefix with mod ID: `examplemod$onTick`
- Use descriptive names: `examplemod$preventDamageIfProtected`

## @Inject Patterns

### Method Start
```java
@Inject(method = "tick", at = @At("HEAD"))
private void modid$onTickStart(CallbackInfo ci) { }
```

### Method End
```java
@Inject(method = "tick", at = @At("TAIL"))
private void modid$onTickEnd(CallbackInfo ci) { }
```

### Before Method Call
```java
@Inject(method = "render", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
private void modid$beforeDrawString(CallbackInfo ci) { }
```

### Cancellable
```java
@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
private void modid$onAttack(Entity target, CallbackInfo ci) {
    if (shouldCancel(target)) {
        ci.cancel();
    }
}
```

### Modify Return Value
```java
@Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
private void modid$modifyHealth(CallbackInfoReturnable<Float> cir) {
    cir.setReturnValue(cir.getReturnValue() * 2);
}
```

## @At Injection Points

| Value | Description |
|-------|-------------|
| `HEAD` | Start of method |
| `TAIL` | End of method (before final return) |
| `RETURN` | At each return statement |
| `INVOKE` | Before/after method call |
| `INVOKE_ASSIGN` | After method call that stores result |
| `FIELD` | Field access (GET/PUT) |
| `NEW` | Object instantiation |
| `CONSTANT` | Constant value usage |

### Using ordinal and shift
```java
// Target specific occurrence
@At(value = "INVOKE", target = "...", ordinal = 2)

// Shift after the target
@At(value = "INVOKE", target = "...", shift = At.Shift.AFTER)
```

## Method Signature Format

```
Lpackage/ClassName;methodName(ParameterTypes)ReturnType

Examples:
- void method():                    "method()V"
- int method(String s):             "method(Ljava/lang/String;)I"
- List<Item> method():              "method()Ljava/util/List;"
- void method(int i, float f):      "method(IF)V"
```

### Type Descriptors
| Type | Descriptor |
|------|------------|
| void | V |
| boolean | Z |
| byte | B |
| char | C |
| short | S |
| int | I |
| long | J |
| float | F |
| double | D |
| Object | Lpackage/ClassName; |
| Array | [Type |

## Mixin JSON Configuration

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.example.mod.mixin",
  "refmap": "${mod_id}.refmap.json",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "MixinLevel",
    "MixinEntity"
  ],
  "client": [
    "MixinMinecraft",
    "MixinGameRenderer"
  ],
  "server": [
    "MixinDedicatedServer"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

**Critical:** Client mixins MUST be in `"client"` array, not `"mixins"`.

## @Shadow Usage

```java
@Mixin(SomeClass.class)
public abstract class MixinSomeClass {

    @Shadow
    private int privateField;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    protected abstract void protectedMethod();

    @Inject(method = "tick", at = @At("HEAD"))
    private void modid$onTick(CallbackInfo ci) {
        this.privateField++;
        this.protectedMethod();
    }
}
```

## @Accessor and @Invoker

For interface-based access without injection:

```java
@Mixin(SomeClass.class)
public interface SomeClassAccessor {

    @Accessor("privateField")
    int getPrivateField();

    @Accessor("privateField")
    void setPrivateField(int value);

    @Invoker("privateMethod")
    void invokePrivateMethod();
}

// Usage
int value = ((SomeClassAccessor) instance).getPrivateField();
```

## Debugging

### Enable Debug Output
Add JVM argument: `-Dmixin.debug=true`

### Common Issues

| Issue | Solution |
|-------|----------|
| Mixin not applying | Check package path in JSON |
| Method not found | Verify method signature |
| ClassNotFound on server | Move mixin to `client` array |
| Conflict with other mod | Lower priority or change approach |

## Best Practices

1. **Be specific** - Use ordinal/shift to target exact locations
2. **Handle null** - Check for null in captured locals
3. **Document targets** - Comment what vanilla behavior you're modifying
4. **Test compatibility** - Check with popular mods in your ecosystem
5. **Use interfaces** - For accessor mixins, use interface mixins
6. **Avoid @Overwrite** - It breaks other mods
