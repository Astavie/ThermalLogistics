package astavie.thermallogistics.util.link;

import astavie.thermallogistics.gui.client.tab.TabLink;
import astavie.thermallogistics.util.reference.CrafterReference;

import java.util.List;

public interface ILink {

	void drawSummary(TabLink tab, int x, int y, int mouseX, int mouseY);

	void addTooltip(TabLink tab, int x, int y, int mouseX, int mouseY, List<String> list);

	CrafterReference<?> getReference();

}
