package dev.lemonclient.addon.managers;

import dev.lemonclient.addon.managers.impl.*;

public class Managers {

    public static final HoldingManager HOLDING = new HoldingManager();
    public static final NotificationManager NOTIFICATION = new NotificationManager();
    public static final OnGroundManager ON_GROUND = new OnGroundManager();
    public static final PingSpoofManager PING_SPOOF = new PingSpoofManager();
    public static final RotationManager ROTATION = new RotationManager();
}
