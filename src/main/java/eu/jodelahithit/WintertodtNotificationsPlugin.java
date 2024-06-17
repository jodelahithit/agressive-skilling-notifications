package eu.jodelahithit;

import com.google.inject.Provides;

import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static eu.jodelahithit.WintertodtInterruptType.*;
import static net.runelite.api.AnimationID.*;
import static net.runelite.api.ItemID.BRUMA_KINDLING;
import static net.runelite.api.ItemID.BRUMA_ROOT;

@PluginDescriptor(
        name = "Wintertodt Notifications",
        description = "Visual notifications for Wintertodt",
        tags = {"notifications", "wintertodt"}
)
public class WintertodtNotificationsPlugin extends Plugin {
    private static final int WINTERTODT_REGION = 6462;
    private boolean isInWintertodt;
    private Instant walkingInstant = Instant.now();
    private Instant lastActionTime = Instant.now();
    private WintertodtActivity currentActivity = WintertodtActivity.IDLE;
    private int timerValue;
    private LocalPoint lastPlayerLocation;

    @Inject
    Client client;

    @Inject
    private WintertodtNotificationsConfig config;

    @Inject
    private WintertodtNotificationsOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    WintertodtNotificationsConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(WintertodtNotificationsConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        currentActivity = WintertodtActivity.IDLE;
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        currentActivity = WintertodtActivity.IDLE;
    }

    private boolean isInWintertodtRegion() {
        if (client.getLocalPlayer() != null) {
            return client.getLocalPlayer().getWorldLocation().getRegionID() == WINTERTODT_REGION;
        }

        return false;
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (!isInWintertodtRegion()) {
            if (isInWintertodt) {
                currentActivity = WintertodtActivity.IDLE;
            }

            isInWintertodt = false;
            return;
        }

        if (!isInWintertodt) {
            currentActivity = WintertodtActivity.IDLE;
        }
        isInWintertodt = true;

        Player player = client.getLocalPlayer();
        if (player != null) {
            LocalPoint playerLocation = player.getLocalLocation();
            if (!playerLocation.equals(lastPlayerLocation)) {
                walkingInstant = Instant.now();
            }
            lastPlayerLocation = playerLocation;
        }

        checkActionTimeout();
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged) {
        timerValue = client.getVar(Varbits.WINTERTODT_TIMER);
    }

    private void checkActionTimeout() {
        if (currentActivity == WintertodtActivity.IDLE) {
            return;
        }

        int currentAnimation = client.getLocalPlayer() != null ? client.getLocalPlayer().getAnimation() : -1;
        if (currentAnimation != IDLE || lastActionTime == null) {
            return;
        }

        Duration actionTimeout = Duration.ofSeconds(3);
        Duration sinceAction = Duration.between(lastActionTime, Instant.now());

        if (sinceAction.compareTo(actionTimeout) >= 0) {
            currentActivity = WintertodtActivity.IDLE;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (!isInWintertodt) {
            return;
        }

        ChatMessageType chatMessageType = chatMessage.getType();

        if (chatMessageType != ChatMessageType.GAMEMESSAGE && chatMessageType != ChatMessageType.SPAM) {
            return;
        }

        MessageNode messageNode = chatMessage.getMessageNode();
        final WintertodtInterruptType interruptType;

        if (messageNode.getValue().startsWith("You carefully fletch the root")) {
            setActivity(WintertodtActivity.FLETCHING);
            return;
        }

        if (messageNode.getValue().startsWith("The cold of")) {
            interruptType = COLD;
        } else if (messageNode.getValue().startsWith("The freezing cold attack")) {
            interruptType = SNOWFALL;
        } else if (messageNode.getValue().startsWith("The brazier is broken and shrapnel")) {
            interruptType = BRAZIER;
        } else if (messageNode.getValue().startsWith("You have run out of bruma roots")) {
            interruptType = WintertodtInterruptType.OUT_OF_ROOTS;
        } else if (messageNode.getValue().startsWith("Your inventory is too full")) {
            interruptType = WintertodtInterruptType.INVENTORY_FULL;
        } else if (messageNode.getValue().startsWith("You fix the brazier")) {
            interruptType = WintertodtInterruptType.FIXED_BRAZIER;
        } else if (messageNode.getValue().startsWith("You light the brazier")) {
            interruptType = WintertodtInterruptType.LIT_BRAZIER;
        } else if (messageNode.getValue().startsWith("The brazier has gone out.")) {
            interruptType = WintertodtInterruptType.BRAZIER_WENT_OUT;
        } else if (messageNode.getValue().startsWith("Congratulations, you've just advanced your")) {
            interruptType = WintertodtInterruptType.LEVEL_UP;
        } else {
            return;
        }

        boolean wasInterrupted = false;

        if (interruptType == SNOWFALL || interruptType == COLD || interruptType == BRAZIER) {
            if (currentActivity != WintertodtActivity.WOODCUTTING && currentActivity != WintertodtActivity.IDLE) {
                wasInterrupted = true;
            }
        } else wasInterrupted = true;

        if (wasInterrupted) {
            currentActivity = WintertodtActivity.IDLE;
        }
    }

    @Subscribe
    public void onAnimationChanged(final AnimationChanged event) {
        if (!isInWintertodt) {
            return;
        }

        final Player local = client.getLocalPlayer();

        if (event.getActor() != local) {
            return;
        }

        final int animId = local.getAnimation();
        switch (animId) {
            case WOODCUTTING_BRONZE:
            case WOODCUTTING_IRON:
            case WOODCUTTING_STEEL:
            case WOODCUTTING_BLACK:
            case WOODCUTTING_MITHRIL:
            case WOODCUTTING_ADAMANT:
            case WOODCUTTING_RUNE:
            case WOODCUTTING_GILDED:
            case WOODCUTTING_DRAGON:
            case WOODCUTTING_DRAGON_OR:
            case WOODCUTTING_INFERNAL:
            case WOODCUTTING_3A_AXE:
            case WOODCUTTING_CRYSTAL:
            case WOODCUTTING_TRAILBLAZER:
            case WOODCUTTING_2H_BRONZE:
	    case WOODCUTTING_2H_IRON:
	    case WOODCUTTING_2H_STEEL:	
	    case WOODCUTTING_2H_BLACK:
	    case WOODCUTTING_2H_MITHRIL:
	    case WOODCUTTING_2H_ADAMANT:
	    case WOODCUTTING_2H_RUNE:
	    case WOODCUTTING_2H_DRAGON:
	    case WOODCUTTING_2H_3A:
	    case WOODCUTTING_2H_CRYSTAL:
	    case WOODCUTTING_2H_CRYSTAL_INACTIVE:
                setActivity(WintertodtActivity.WOODCUTTING);
                break;

            case FLETCHING_BOW_CUTTING:
                setActivity(WintertodtActivity.FLETCHING);
                break;

            case LOOKING_INTO:
                setActivity(WintertodtActivity.FEEDING_BRAZIER);
                break;

            case FIREMAKING:
                setActivity(WintertodtActivity.LIGHTING_BRAZIER);
                break;

            case CONSTRUCTION:
            case CONSTRUCTION_IMCANDO:
                setActivity(WintertodtActivity.FIXING_BRAZIER);
                break;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final ItemContainer container = event.getItemContainer();
        if (!isInWintertodt || container != client.getItemContainer(InventoryID.INVENTORY)) {
            return;
        }
        int numLogs = 0;
        int numKindling = 0;

        final Item[] inv = container.getItems();
        for (Item item : inv) {
            switch (item.getId()) {
                case BRUMA_ROOT:
                    ++numLogs;
                    break;
                case BRUMA_KINDLING:
                    ++numKindling;
                    break;
            }
        }

        if (numLogs == 0 && currentActivity == WintertodtActivity.FLETCHING) {
            currentActivity = WintertodtActivity.IDLE;
        } else if (numLogs == 0 && numKindling == 0 && currentActivity == WintertodtActivity.FEEDING_BRAZIER) {
            currentActivity = WintertodtActivity.IDLE;
        }
    }

    private void setActivity(WintertodtActivity action) {
        if (action != WintertodtActivity.IDLE) {
            walkingInstant = walkingInstant.minus(1, ChronoUnit.MINUTES);
        }
        currentActivity = action;
        lastActionTime = Instant.now();
    }

    public boolean shouldRenderOverlay() {
        return isInWintertodt && currentActivity == WintertodtActivity.IDLE && timerValue == 0 && !Utils.checkInstant(walkingInstant, 1.0f);
    }
}
