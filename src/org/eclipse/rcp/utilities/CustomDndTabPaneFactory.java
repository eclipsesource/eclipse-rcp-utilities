/*******************************************************************************
 * Copyright (c) 2014 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl<tom.schindl@bestsolution.at> - initial API and implementation
 *******************************************************************************/
package org.eclipse.rcp.utilities;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DragSetup;

public class CustomDndTabPaneFactory {

	public static AbstractTabPane createDndTabPane(Supplier<? extends AbstractTabPane> widgetFactory, Consumer<DragSetup> setup,
			boolean allowDetach) {
		AbstractTabPane abstractTabPane = widgetFactory.get();
		new CustomDndTabPaneWrapper(abstractTabPane, allowDetach, setup);
		return abstractTabPane;
	}
}

