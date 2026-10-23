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

    // Сколько блоков ставишь ты сам, после этого мод ставит следующий.
    // 2 = ты ставишь 2, мод ставит 3-й.
    private static final int USER_BLOCKS = 2;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding toggleKey = new KeyBinding("Toggle Telly Helper", Keyboard.KEY_K, "Telly Helper");

    private boolean enabled = true;
    private boolean wasOnGround = true;
    private boolean placedByMod = false;
    private int userPlaced = 0;

    // для отладки
    private boolean announced = false;
    private String lastReason = "";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void say(String msg) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("[TH] " + msg));
        }
    }

    private void fail(String reason) {
        if (!reason.equals(lastReason)) {
            lastReason = reason;
            say("не ставлю: " + reason);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!enabled || e.button != 1 || !e.buttonstate) return;
        EntityPlayer p = mc.thePlayer;
        if (p == null) return;
        if (mc.currentScreen != null) { say("клик не засчитан: открыто меню"); return; }
        if (p.onGround) { say("клик не засчитан: ты на земле"); return; }
        if (!holdingBlock(p)) { say("клик не засчитан: в руке не блок"); return; }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            userPlaced++;
            say("клик засчитан: " + userPlaced + " из " + USER_BLOCKS);
        } else {
            say("клик не засчитан: прицел не на блоке");
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

        if (!announced) {
            announced = true;
            say("мод загружен, включен: " + enabled + ", ты ставишь " + USER_BLOCKS + ", мод ставит следующий");
        }

        if (p.onGround != wasOnGround) {
            wasOnGround = p.onGround;
            userPlaced = 0;
            placedByMod = false;
            lastReason = "";
        }

        if (!enabled || p.onGround || placedByMod || mc.currentScreen != null) return;
        if (userPlaced != USER_BLOCKS) return;
        if (!holdingBlock(p)) { fail("в руке не блок"); return; }

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            fail("прицел не на блоке");
            return;
        }

        BlockPos target = mop.getBlockPos().offset(mop.sideHit);
        if (!mc.theWorld.isAirBlock(target)) { fail("место занято"); return; }

        AxisAlignedBB box = new AxisAlignedBB(target, target.add(1, 1, 1));
        if (p.getEntityBoundingBox().intersectsWith(box)) { fail("блок упёрся в тебя"); return; }

        ItemStack held = p.getHeldItem();
        if (mc.playerController.onPlayerRightClick(p, mc.theWorld, held, mop.getBlockPos(), mop.sideHit, mop.hitVec)) {
            p.swingItem();
            placedByMod = true;
            say("мод поставил блок");
        } else {
            fail("сервер/игра не приняли клик");
        }
    }

    private boolean holdingBlock(EntityPlayer p) {
        ItemStack s = p.getHeldItem();
        return s != null && s.getItem() instanceof ItemBlock;
    }
    }
                
