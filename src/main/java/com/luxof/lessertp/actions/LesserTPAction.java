package com.luxof.lessertp.actions;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation;
import at.petrak.hexcasting.api.casting.mishaps.MishapEntityTooFarAway;
import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.common.casting.actions.spells.great.OpTeleport;

import com.luxof.lessertp.MishapThrowerJava;

import com.mojang.datafixers.util.Either;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class LesserTPAction implements SpellAction {
    public int getArgc() {
        return 2;
    }

    // why
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment ctx) {
        Entity teleportee = OperatorUtils.getEntity(args, 0, getArgc());
        try { ctx.assertEntityInRange(teleportee); }
        catch (MishapEntityTooFarAway e) { MishapThrowerJava.throwMishap(e); }

        Either<Double, Vec3d> arg2 = OperatorUtils.getNumOrVec(args, 1, getArgc());
        Vec3d fract;
        if (arg2.left().isPresent()) {
            Double num = clamp(arg2.left().get(), 0.001, 0.999);
            fract = new Vec3d(num, num, num);
        } else {
            Vec3d vec = arg2.right().get();
            fract = new Vec3d(
                clamp(vec.x, 0.001, 0.999),
                clamp(vec.y, 0.001, 0.999),
                clamp(vec.z, 0.001, 0.999)
            );
        }

        if(!HexConfig.server().canTeleportInThisDimension(ctx.getWorld().getRegistryKey()))
            MishapThrowerJava.throwMishap(new MishapBadLocation(fract, "bad_dimension"));

		return new SpellAction.Result(
            new Spell(teleportee, fract),
            (long)(MediaConstants.DUST_UNIT * 0.01),
            List.of(ParticleSpray.burst(ctx.mishapSprayPos(), 3, 10)),
            1
        );
    }

    public static class Spell implements RenderedSpell {
        private final Entity teleportee;
        private final Vec3d fract;

        public Spell(Entity teleportee, Vec3d fract) {
            this.teleportee = teleportee;
            this.fract = fract;
        }

		@Override
		public void cast(CastingEnvironment ctx) {
            Vec3d entityPos = teleportee.getPos();
			// negative coordinates :tasuqe_waaanager:
			Vec3d newFract = new Vec3d(
				entityPos.x > 0 ? fract.x : 1 - fract.x,
				entityPos.y > 0 ? fract.y : 1 - fract.y,
				entityPos.z > 0 ? fract.z : 1 - fract.z
			);
			Vec3d offset = new Vec3d(
				Math.floor(entityPos.x),
				Math.floor(entityPos.y),
				Math.floor(entityPos.z)
			).add(newFract).subtract(entityPos);

			OpTeleport.INSTANCE.teleportRespectSticky(teleportee, offset, ctx.getWorld());
		}

        @Override
        public CastingImage cast(CastingEnvironment arg0, CastingImage arg1) {
            return RenderedSpell.DefaultImpls.cast(this, arg0, arg1);
        }
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment ctx) {
        return SpellAction.DefaultImpls.awardsCastingStat(this, ctx);
    }

    @Override
    public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, NbtCompound userData) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userData);
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment ctx) {
        return SpellAction.DefaultImpls.hasCastingSound(this, ctx);
    }

    @Override
    public OperationResult operate(CastingEnvironment arg0, CastingImage arg1, SpellContinuation arg2) {
        return SpellAction.DefaultImpls.operate(this, arg0, arg1, arg2);
    }
}
