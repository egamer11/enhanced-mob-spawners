package com.branders.spawnermod.gui;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;
import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.networking.SpawnerModPacketHandler;
import com.branders.spawnermod.networking.packet.SyncPreciseSpawnerMessage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Precise Spawner GUI config screen. Similar to spawner config but with additional
 * delay configuration and mob tracking display.
 *
 * @author Anders <Branders> Blomqvist
 */
@OnlyIn(Dist.CLIENT)
public class PreciseSpawnerConfigGui extends Screen {

    private static final Component titleText = Component.translatable("gui.spawnermod.precise_spawner_config_screen_title");

    // Used for rendering.
    private Minecraft minecraft = Minecraft.getInstance();

    // References to Precise Spawner Logic. Set in constructor
    private PreciseSpawnerBlockEntity preciseSpawner;
    private BlockPos pos;

    // GUI Texture
    private ResourceLocation spawnerConfigTexture = new ResourceLocation(
            SpawnerMod.MOD_ID, "/textures/gui/spawner_config_screen.png");
    private int imageWidth = 178;
    private int imageHeight = 177;

    // Buttons for controlling Precise Spawner data
    private Button delayButton = null;
    private Button speedButton = null;
    private Button rangeButton = null;
    private Button disableButton = null;
    private Button mobLimitButton = null;

    // Button States
    private int delayOptionValue;
    private int speedOptionValue;
    private int rangeOptionValue;
    private int mobLimit;

    // What the button will display depending on option value
    String[] delayDisplayString = {"Very Fast", "Fast", "Default", "Slow", "Very Slow"};
    String[] speedDisplayString = {"Slow", "Default", "Fast", "Very Fast"};
    String[] rangeDisplayString = {"Default", "Far", "Very Far", "Extreme"};
    String[] disableDisplayString = {"Enabled", "Disabled"};

    /**
     * Object to hold values for all configuration parameters.
     */
    private class Data {
        int LOW, DEFAULT, HIGH, HIGHEST, EXTREME;
        public Data(int i, int j, int k, int l, int m) {
            LOW = i;
            DEFAULT = j;
            HIGH = k;
            HIGHEST = l;
            EXTREME = m;
        }
        public Data(int i, int j, int k, int l) {
            LOW = i;
            DEFAULT = j;
            HIGH = k;
            HIGHEST = l;
            EXTREME = l;
        }
    }

    // Create the data for precise spawner configuration
    private Data _customDelay = new Data(20, 50, 100, 200, 400); // In ticks (1-20 seconds)
    private Data _minSpawnDelay = new Data(300, 200, 100, 50);
    private Data _maxSpawnDelay = new Data(900, 800, 400, 100);
    private Data _requiredPlayerRange = new Data(16, 32, 64, 128);

    // Create the variables which holds current configuration values
    private int customDelay;
    private int minSpawnDelay;
    private int maxSpawnDelay;
    private int requiredPlayerRange;
    private boolean disabled;

    private boolean cachedDisabled;

    public PreciseSpawnerConfigGui(Component textComponent, PreciseSpawnerBlockEntity preciseSpawner, BlockPos pos) {
        super(textComponent);

        this.preciseSpawner = preciseSpawner;
        this.pos = pos;

        // Read current values from precise spawner
        this.customDelay = preciseSpawner.getCustomDelay();
        this.minSpawnDelay = preciseSpawner.getMinSpawnDelay();
        this.maxSpawnDelay = preciseSpawner.getMaxSpawnDelay();
        this.requiredPlayerRange = preciseSpawner.getRequiredPlayerRange();
        this.disabled = preciseSpawner.isDisabled();
        this.cachedDisabled = this.disabled;
        this.mobLimit = preciseSpawner.getMobLimit();

        // Load button configuration
        delayOptionValue = loadDelayOptionState(customDelay);
        speedOptionValue = loadOptionState(minSpawnDelay, _minSpawnDelay);
        rangeOptionValue = loadOptionState(requiredPlayerRange, _requiredPlayerRange);
    }

    @Override
    public void init() {
        /**
         * Custom Delay button (specific to precise spawner)
         */
        delayButton = addRenderableWidget(Button.builder(
                Component.translatable("button.delay." + getDelayButtonText(delayOptionValue)), button -> {
                    switch(delayOptionValue) {
                    // Very Fast, set to Fast
                    case 0:
                        delayOptionValue = 1;
                        customDelay = _customDelay.DEFAULT;
                        break;

                    // Fast, set to Default
                    case 1:
                        delayOptionValue = 2;
                        customDelay = _customDelay.HIGH;
                        break;

                    // Default, set to Slow
                    case 2:
                        delayOptionValue = 3;
                        customDelay = _customDelay.HIGHEST;
                        break;

                    // Slow, set to Very Slow
                    case 3:
                        delayOptionValue = 4;
                        customDelay = _customDelay.EXTREME;
                        break;

                    // Very Slow, set back to Very Fast
                    case 4:
                        delayOptionValue = 0;
                        customDelay = _customDelay.LOW;
                        break;
                    }
                    delayButton.setMessage(Component.translatable("button.delay." + getDelayButtonText(delayOptionValue)));
                })
                .bounds(width / 2 - 48, 55, 108, 20)
                .build());

        /**
         * Speed button
         */
        speedButton = addRenderableWidget(Button.builder(
                Component.translatable("button.speed." + getButtonText(speedOptionValue)), button -> {
                    switch(speedOptionValue) {
                    // Slow, set to default
                    case 0:
                        speedOptionValue = 1;
                        minSpawnDelay = _minSpawnDelay.DEFAULT;
                        maxSpawnDelay = _maxSpawnDelay.DEFAULT;
                        break;

                    // Default, set to Fast
                    case 1:
                        speedOptionValue = 2;
                        minSpawnDelay = _minSpawnDelay.HIGH;
                        maxSpawnDelay = _maxSpawnDelay.HIGH;
                        break;

                    // High, set to Very Fast
                    case 2:
                        speedOptionValue = 3;
                        minSpawnDelay = _minSpawnDelay.HIGHEST;
                        maxSpawnDelay = _maxSpawnDelay.HIGHEST;
                        break;

                    // Very high, set back to Slow
                    case 3:
                        speedOptionValue = 0;
                        minSpawnDelay = _minSpawnDelay.LOW;
                        maxSpawnDelay = _maxSpawnDelay.LOW;
                        break;
                    }
                    speedButton.setMessage(Component.translatable("button.speed." + getButtonText(speedOptionValue)));
                })
                .bounds(width / 2 - 48, 80, 108, 20)
                .build());

        /**
         * Range button
         */
        rangeButton = addRenderableWidget(Button.builder(
                Component.translatable("button.range." + getButtonText(rangeOptionValue)).append(" " + requiredPlayerRange), button -> {
                    switch(rangeOptionValue) {
                    // Default, set to Far
                    case 0:
                        rangeOptionValue = 1;
                        requiredPlayerRange = _requiredPlayerRange.DEFAULT;
                        break;

                    // Far, set to Very Far
                    case 1:
                        rangeOptionValue = 2;
                        requiredPlayerRange = _requiredPlayerRange.HIGH;
                        break;

                    // Very Far, set to Extreme
                    case 2:
                        rangeOptionValue = 3;
                        requiredPlayerRange = _requiredPlayerRange.HIGHEST;
                        break;

                    // Extreme, set back to Default
                    case 3:
                        rangeOptionValue = 0;
                        requiredPlayerRange = _requiredPlayerRange.LOW;
                        break;
                    }

                    rangeButton.setMessage(Component.translatable("button.range." + getButtonText(rangeOptionValue)).append(" " + requiredPlayerRange));
                })
                .bounds(width / 2 - 48, 105, 108, 20)
                .build());

        /**
         * Mob Limit button
         */
        mobLimitButton = addRenderableWidget(Button.builder(
                Component.literal("Mob Limit: " + mobLimit), button -> {
                    mobLimit++;
                    if(mobLimit > 10) {
                        mobLimit = 1;
                    }
                    mobLimitButton.setMessage(Component.literal("Mob Limit: " + mobLimit));
                })
                .bounds(width / 2 - 48, 130, 108, 20)
                .build());

        /**
         * Disable button
         */
        disableButton = addRenderableWidget(Button.builder(
                Component.translatable("button.toggle." + getButtonText(disabled)), button -> {
                    if(disabled) {
                        // Set spawner to ON
                        disabled = false;
                        toggleButtons(true);
                        // Restore range setting
                        switch(rangeOptionValue) {
                        case 0:
                            requiredPlayerRange = _requiredPlayerRange.LOW;
                            break;
                        case 1:
                            requiredPlayerRange = _requiredPlayerRange.DEFAULT;
                            break;
                        case 2:
                            requiredPlayerRange = _requiredPlayerRange.HIGH;
                            break;
                        case 3:
                            requiredPlayerRange = _requiredPlayerRange.HIGHEST;
                            break;
                        }
                    }
                    else {
                        // Set spawner OFF
                        disabled = true;
                        toggleButtons(false);
                        requiredPlayerRange = 0;
                    }

                    disableButton.setMessage(Component.translatable("button.toggle." + getButtonText(disabled)));
                })
                .bounds(width / 2 - 48, 155, 108, 20)
                .build());

        /**
         * Save button - configures precise spawner data
         */
        addRenderableWidget(Button.builder(Component.translatable("button.save"), button -> {
            configurePreciseSpawner();
            this.close();
        }).bounds(width / 2 - 89, 180 + 10, 178, 20).build());

        /**
         * Cancel button
         */
        addRenderableWidget(Button.builder(Component.translatable("button.cancel"), button -> {
            this.close();
        }).bounds(width / 2 - 89, 180 + 35, 178, 20).build());

        if(disabled)
            toggleButtons(false);
        else
            toggleButtons(true);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw black transparent background
        renderBackground(context);

        // Draw spawner screen texture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, spawnerConfigTexture);
        context.blit(spawnerConfigTexture, width / 2 - imageWidth / 2, 5, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        // Render title text
        int length = titleText.getString().length() * 2;
        context.drawString(minecraft.font, titleText, width / 2 - length - 3, 33, 0xFFD964);


        super.render(context, mouseX, mouseY, partialTicks);
    }

    private void close() {
        minecraft.setScreen((Screen)null);
    }

    /**
     * Send message to server with the new configuration values.
     */
    private void configurePreciseSpawner() {
        if(cachedDisabled && cachedDisabled == disabled) {
            return;
        }

        SpawnerModPacketHandler.INSTANCE.sendToServer(
                new SyncPreciseSpawnerMessage(
                        pos,
                        customDelay,
                        minSpawnDelay,
                        maxSpawnDelay,
                        requiredPlayerRange,
                        disabled,
                        mobLimit));
    }

    private String getButtonText(int optionValue) {
        switch(optionValue) {
        case 0:
            return "low";
        case 1:
            return "default";
        case 2:
            return "high";
        case 3:
            return "very_high";
        case 4:
            return "custom";
        default:
            return "default";
        }
    }

    private String getDelayButtonText(int optionValue) {
        switch(optionValue) {
        case 0:
            return "very_fast";
        case 1:
            return "fast";
        case 2:
            return "default";
        case 3:
            return "slow";
        case 4:
            return "very_slow";
        default:
            return "default";
        }
    }

    private String getButtonText(boolean disabled) {
        if(disabled)
            return "disabled";
        else
            return "enabled";
    }

    /**
     * Loads configuration state for delay button
     */
    private int loadDelayOptionState(int current) {
        if(current == _customDelay.LOW)
            return 0;
        else if(current == _customDelay.DEFAULT)
            return 1;
        else if(current == _customDelay.HIGH)
            return 2;
        else if(current == _customDelay.HIGHEST)
            return 3;
        else if(current == _customDelay.EXTREME)
            return 4;
        else
            return 2; // Default
    }

    /**
     * Loads configuration state for other buttons
     */
    private int loadOptionState(int current, Data reference) {
        if(current == reference.LOW)
            return 0;
        else if(current == reference.DEFAULT)
            return 1;
        else if(current == reference.HIGH)
            return 2;
        else if(current == reference.HIGHEST)
            return 3;
        else
            return 0;
    }

    /**
     * Toggles buttons based on enabled/disabled state
     */
    private void toggleButtons(boolean state) {
        if(ConfigValues.get("disable_spawner_config") != 0) {
            delayButton.active = false;
            speedButton.active = false;
            rangeButton.active = false;
            mobLimitButton.active = false;
            return;
        }

        delayButton.active = state;
        mobLimitButton.active = state;

        if(ConfigValues.get("disable_speed") != 0) {
            speedButton.active = false;
            speedButton.setMessage(Component.translatable("button.speed.disabled"));
        } else {
            speedButton.active = state;
        }

        if(ConfigValues.get("disable_range") != 0) {
            rangeButton.active = false;
            rangeButton.setMessage(Component.translatable("button.range.disabled"));
        } else {
            rangeButton.active = state;
        }
    }
}