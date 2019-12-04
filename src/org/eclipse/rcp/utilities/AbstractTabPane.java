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

import javafx.beans.property.DoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.StackPane;

/**
 * Generic API for a custom JavaFX Tab Pane
 */
public abstract class AbstractTabPane extends StackPane {

	public abstract DoubleProperty minTabWidthProperty();

	public abstract void setSide(Side v);

	public abstract TabClosingPolicy getTabClosingPolicy();

	public abstract void setTabClosingPolicy(TabClosingPolicy policy);

	public abstract SingleSelectionModel<AbstractTab> getSelectionModel();

	public abstract ObservableList<AbstractTab> getTabs();

}
