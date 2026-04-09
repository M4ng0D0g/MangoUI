# Camera & Perspective API

## Overview
MyuLib camera system provides server-to-client camera actions, local camera modifiers, smooth path moves, and shake effects.

## Core components
- `CameraApi`: public facade for server and local camera actions.
- `CameraTrackingTarget`: encapsulates position/entity tracking with offset and optional look target.
- `CameraModifier`: effect interface.
- `CameraActionPayload`: action record for command/network transport.
- `ClientCameraManager`: client singleton that applies modifier stacks every camera update.
- `ShakeModifier`: damped shake effect.
- `PathAnimationModifier`: eased camera movement to a target.
- `CameraMixin`: injects camera update tail and applies modifiers.

## Current implementation notes
- Server dispatch is bridged via `CameraDispatchBridge`.
- `ClientCameraBridge.dispatch(...)` currently falls back to local apply and is ready for Fabric custom payload wiring.

## Example
```java
CameraApi.shake(player, 1.2f, 1200L);
CameraApi.moveTo(player, CameraTrackingTarget.of(player).withOffset(new Vec3(0, 3, -6)), 1500L, Easing.EASE_IN_OUT_QUAD);
CameraApi.reset(player);
```

