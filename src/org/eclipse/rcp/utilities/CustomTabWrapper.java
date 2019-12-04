/*******************************************************************************
 * Copyright (c) 2015 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl<tom.schindl@bestsolution.at> - initial API and implementation
 *******************************************************************************/
package org.eclipse.rcp.utilities;

import org.eclipse.fx.ui.controls.tabpane.GenericTab;
import org.eclipse.fx.ui.controls.tabpane.GenericTabPane;

public class CustomTabWrapper implements GenericTab {

	private AbstractTab tab;

	public CustomTabWrapper(AbstractTab tab) {
		this.tab = tab;
	}

	@Override
	public GenericTabPane getOwner() {
		AbstractTabPane tabPane = this.tab.getTabPane();
		if (tabPane == null) {
			return null;
		}
		return CustomTabPaneWrapper.wrap(tabPane);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeInstance() {
		return (T) tab;
	}

	@Override
	public Object getUserData() {
		return tab.getUserData();
	}

	public static GenericTab wrap(AbstractTab tab) {
		return new CustomTabWrapper(tab);
	}

}
