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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

/**
 * Generic API for a custom JavaFX Tab Pane
 */
public abstract class AbstractTab extends StackPane {

	public abstract StringProperty textProperty();

	public abstract void setContextMenu(ContextMenu contextMenu);

	public abstract void setTooltip(Tooltip tooltip);

	public abstract void setGraphic(Node graphic);

	public abstract void setClosable(boolean closable);

	public abstract void setText(String text);

	public abstract void setTabPane(AbstractTabPane tabPane);

	public abstract AbstractTabPane getTabPane();

	public abstract Node getContent();

	public abstract ObjectProperty<Node> contentProperty();

	public abstract void setContent(Node content);

	public abstract void setOnCloseRequest(EventHandler<Event> value);

	public abstract ObjectProperty<EventHandler<Event>> onCloseRequestProperty();

	public abstract boolean isSelected();

	public abstract void updateCloseButtonState();

}
