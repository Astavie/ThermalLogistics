package astavie.thermallogistics.util.link;

import astavie.thermallogistics.gui.client.tab.TabLink;

public interface ILink {

	Runnable drawSummary(TabLink tab, int x, int y, int mouseX, int mouseY);

}
