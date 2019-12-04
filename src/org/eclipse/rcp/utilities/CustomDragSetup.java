/*******************************************************************************
 * Copyright (c) 2019 EclipseSource Muenchen GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 ******************************************************************************/
package org.eclipse.rcp.utilities;

import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DragSetup;

public interface CustomDragSetup extends DragSetup {
	public void dispose();
}
