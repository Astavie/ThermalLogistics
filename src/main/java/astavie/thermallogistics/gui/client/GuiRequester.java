package astavie.thermallogistics.gui.client;

import astavie.thermallogistics.gui.client.tab.TabRequests;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import astavie.thermallogistics.util.request.Requests;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.gui.client.GuiDuctConnection;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.List;

public class GuiRequester<T extends DuctUnit<T, ?, ?>, I> extends GuiDuctConnection {

	private final IDelegateClient<I, ?> delegate;
	private final List<Requests<T, I>> requests;
	private final Runnable refresh;

	public GuiRequester(InventoryPlayer inventory, ConnectionBase conBase, IDelegateClient<I, ?> delegate, List<Requests<T, I>> requests, Runnable refresh) {
		super(inventory, conBase);
		this.delegate = delegate;
		this.requests = requests;
		this.refresh = refresh;
	}

	@Override
	public void initGui() {
		super.initGui();
		addTab(new TabRequests<>(this, delegate, requests, refresh));
	}

}
