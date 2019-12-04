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

public class CustomTabPaneWrapper implements GenericTabPane {

	private AbstractTabPane tabPane;

	public CustomTabPaneWrapper(AbstractTabPane tabPane) {
		this.tabPane = tabPane;
	}

	@Override
	public int indexOf(GenericTab t) {
		return tabPane.getTabs().indexOf(t.getNativeInstance());
	}

	@Override
	public boolean remove(GenericTab t) {
		return tabPane.getTabs().remove(t.getNativeInstance());
	}

	@Override
	public void add(GenericTab t) {
		tabPane.getTabs().add(t.getNativeInstance());
	}

	@Override
	public void add(int index, GenericTab t) {
		tabPane.getTabs().add(index, t.getNativeInstance());
	}

	@Override
	public int getTabNumber() {
		return tabPane.getTabs().size();
	}

	@Override
	public void select(GenericTab draggedTab) {
		tabPane.getSelectionModel().select(draggedTab.getNativeInstance());
	}

	public static GenericTabPane wrap(AbstractTabPane tabPane) {
		return new CustomTabPaneWrapper(tabPane);
	}

}
