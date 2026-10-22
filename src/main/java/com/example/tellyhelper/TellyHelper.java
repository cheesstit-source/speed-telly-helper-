package com.example.tellyhelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "tellyhelper", name = "Telly Helper", version = "1.0", clientSideOnly = true)
public class TellyHelper {

    private static final int USER_BLOCKS = 3;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding toggleKey = new KeyBinding("Toggle Telly Helper", Keyboard.KEY_K, "Telly Helper");

    private boolean enabled = true;
    private boolean wasOnGround = true;
    private boolean placedByMod = false;
    private int userPlaced = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!enabled || e.button != 1 || !e.buttonstate) return;
        EntityPlayer p = mc.thePlayer;
        if (p == null || mc.currentScreen != null || p.onGround) return;
        if (!holdingBlock(p)) return;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            userPlaced++;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        if (toggleKey.isPressed()) {
            enabled = !enabled;
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText("Telly Helper: " + (enabled ? "ON" : "OFF")));
            }
        }

        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) return;

        if (p.onGround != wasOnGround) {
            wasOnGround = p.onGround;
            userPlaced = 0;
            placedByMod = false;
        }

        if (!enabled || p.onGround || placedByMod || mc.currentScreen != null) return;
        if (userPlaced != USER_BLOCKS) return;
        if (!holdingBlock(p)) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        BlockPos target = mop.getBlockPos().offset(mop.sideHit);
        if (!mc.theWorld.isAirBlock(target)) return;

        AxisAlignedBB box = new AxisAlignedBB(target, target.add(1, 1, 1));
        if (p.getEntityBoundingBox().intersectsWith(box)) return;

        ItemStack held = p.getHeldItem();
        if (mc.playerController.onPlayerRightClick(p, mc.theWorld, held, mop.getBlockPos(), mop.sideHit, mop.hitVec)) {
            p.swingItem();
            placedByMod = true;
        }
    }

    private boolean holdingBlock(EntityPlayer p) {
        ItemStack s = p.getHeldItem();
        return s != null && s.getItem() instanceof ItemBlock;
    }
}
