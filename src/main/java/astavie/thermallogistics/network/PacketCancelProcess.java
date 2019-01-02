package astavie.thermallogistics.network;

import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.request.Requests;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class PacketCancelProcess extends PacketBase {

	public PacketCancelProcess(Requests<?, ?> requests) {
		requests.writeCancel(this);
	}

	public PacketCancelProcess() {
	}

	public static void initialize() {
		PacketHandler.INSTANCE.registerPacket(PacketCancelProcess.class);
	}

	@Override
	public void handlePacket(EntityPlayer player, boolean isServer) {
		int x = getInt();
		int y = getInt();
		int z = getInt();
		int side = getInt();
		int index = getInt();

		TileEntity tile = player.world.getTileEntity(new BlockPos(x, y, z));
		if (tile != null) {
			if (tile instanceof IProcessHolder) {
				((IProcessHolder<?, ?, ?>) tile).getProcesses().get(index).setFailed();
			} else if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(side);
				if (attachment instanceof IProcessHolder)
					((IProcessHolder<?, ?, ?>) attachment).getProcesses().get(index).setFailed();
			}
		}
	}

}
