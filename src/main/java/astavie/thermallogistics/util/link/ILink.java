package astavie.thermallogistics.util.link;

import astavie.thermallogistics.gui.client.tab.TabLink;

import java.util.List;

public interface ILink {

	void drawSummary(TabLink tab, int x, int y, int mouseX, int mouseY);

	void addTooltip(TabLink tab, int x, int y, int mouseX, int mouseY, List<String> list);

}
