package com.mastcraft.voice;

import com.mastcraft.voice.client.ClientVoiceManager;
import com.mastcraft.voice.client.KeyBindings;
import com.mastcraft.voice.client.network.ClientPacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MastCraftVoiceMod.MOD_ID)
public class MastCraftVoiceMod {

    public static final String MOD_ID = "mastcraftvoice";

    private static ClientVoiceManager clientManager;

    public MastCraftVoiceMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ثبت payload شبکه (Forge 1.21.1)
        modBus.addListener(ClientPacketHandler::registerPayloads);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::onClientSetup);
            modBus.addListener(KeyBindings::register);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            clientManager = new ClientVoiceManager();
            clientManager.init();

            MinecraftForge.EVENT_BUS.addListener((GameShuttingDownEvent e) -> {
                if (clientManager != null) {
                    clientManager.shutdown();
                }
            });
        });
    }

    public static ClientVoiceManager getClientManager() {
        return clientManager;
    }
}