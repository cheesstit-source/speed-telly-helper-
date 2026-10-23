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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "tellyhelper", name = "Telly Helper", version = "1.0", clientSideOnly = true)
public class TellyHelper {

    // ===== НАСТРОЙКИ =====
    // Сколько блоков мод ставит за один прыжок
    private static final int MAX_PER_JUMP = 3;
    // Мод сам прыгает, когда ты идёшь вперёд с блоком в руке
    private static final boolean AUTO_JUMP = true;
    // Мод сам включает спринт, когда ты идёшь вперёд с блоком в руке
    private static final boolean AUTO_SPRINT = true;
    // Мод ставит блоки только когда под тобой пустота (ты не над островом или мостом)
    private static final boolean ONLY_OVER_VOID = true;
    // Писать в чат сообщения мода (поставь false, когда всё заработает)
    private static final boolean DEBUG = true;
    // ======================

    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding toggleKey = new KeyBinding("Toggle Telly Helper", Keyboard.KEY_K, "Telly Helper");

    private boolean enabled = true;
    private boolean announced = false;
    private boolean jumpHeldByMod = false;
    private int placedThisJump = 0;
    private String lastReason = "";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void say(String msg) {
        if (DEBUG && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("[TH] " + msg));
        }
    }

    private void fail(String reason) {
        if (!reason.equals(lastReason)) {
            lastReason = reason;
            say("не ставлю: " + reason);
        }
    }

    private void releaseJump() {
        if (jumpHeldByMod) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            jumpHeldByMod = false;
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
            say("мод загружен, включен: " + enabled + ". Ты только поворачиваешь камеру.");
        }

        if (!enabled || mc.currentScreen != null) {
            releaseJump();
            return;
        }

        boolean hasBlock = holdingBlock(p);
        boolean forward = mc.gameSettings.keyBindForward.isKeyDown();

        // автоспринт
        if (AUTO_SPRINT && forward && hasBlock) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }

        // автопрыжок
        if (AUTO_JUMP) {
            if (forward && hasBlock && p.onGround) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                jumpHeldByMod = true;
            } else {
                releaseJump();
            }
        }

        // на земле: новый прыжок, сбрасываем счётчик
        if (p.onGround) {
            placedThisJump = 0;
            lastReason = "";
            return;
        }

        if (!hasBlock) return;
        if (placedThisJump >= MAX_PER_JUMP) return;

        // под тобой должна быть пустота, иначе мод ничего не ставит
        if (ONLY_OVER_VOID) {
            BlockPos under1 = new BlockPos(p.posX, p.posY - 1.0, p.posZ);
            BlockPos under2 = new BlockPos(p.posX, p.posY - 2.0, p.posZ);
            if (!mc.theWorld.isAirBlock(under1) || !mc.theWorld.isAirBlock(under2)) {
                fail("под тобой есть блок");
                return;
            }
        }

        // ставим блок туда, куда ты навёл прицел
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
            placedThisJump++;
            say("поставил блок " + placedThisJump + " из " + MAX_PER_JUMP);
        } else {
            fail("игра не приняла клик");
        }
    }

    private boolean holdingBlock(EntityPlayer p) {
        ItemStack s = p.getHeldItem();
        return s != null && s.getItem() instanceof ItemBlock;
    }
     }
                     
