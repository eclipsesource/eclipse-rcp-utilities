/*******************************************************************************
 * Copyright (c) 2014 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adrodoc55<adrodoc55@googlemail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.rcp.utilities;

import java.util.function.Consumer;

import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DragSetup;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.FeedbackType;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

public class CustomDndTabPaneWrapper {
	private static final FeedbackType DEFAULT_FEEDBACK_TYPE = FeedbackType.MARKER;
	private BooleanProperty allowDetach;
	private ObjectProperty<FeedbackType> feedbackType;
	private AbstractTabPane tabPane;

	private CustomDragSetup setup;

	/**
	 * Create a tab pane and set the drag strategy
	 *
	 * @param allowDetach
	 *        allow detaching
	 * @param setup
	 *        the setup instance for the pane
	 */
	public CustomDndTabPaneWrapper(AbstractTabPane tabPane, boolean allowDetach, Consumer<DragSetup> setup) {
		this(tabPane, DEFAULT_FEEDBACK_TYPE, allowDetach, setup);
	}

	/**
	 * Create a tab pane and set the drag strategy
	 * 
	 * @param tabPane
	 *
	 * @param feedbackType
	 *        the feedback type
	 * @param allowDetach
	 *        allow detaching
	 * @param setup
	 *        the setup instance for the pane
	 */
	public CustomDndTabPaneWrapper(AbstractTabPane tabPane, FeedbackType feedbackType, boolean allowDetach, Consumer<DragSetup> setup) {
		this.tabPane = tabPane;
		setFeedbackType(feedbackType);
		setAllowDetach(allowDetach);
		initListeners(setup);
	}

	private void initListeners(Consumer<DragSetup> setup) {
		tabPane.sceneProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue != null && this.setup != null) {
				this.setup.dispose();
				this.setup = null;
			}
			if (newValue != null) {
				setupDnd(tabPane, setup);
			}
		});
		allowDetachProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				Scene scene = tabPane.getScene();
				if (scene != null) {
					if (this.setup != null) {
						this.setup.dispose();
						this.setup = null;
					}
					setupDnd(tabPane, setup);
				}
			}
		});
	}

	private void setupDnd(AbstractTabPane tabPane, Consumer<DragSetup> setup) {
		if (isAllowDetach()) {
			this.setup = new CustomDnDTabPaneHookerFullDrag(tabPane);
		} else {
			this.setup = new CustomDndTabPaneSkinHooker(tabPane);
		}
		setup.accept(this.setup);
	}

	/**
	 * @return a property indicating whether Tabs can be detached or not
	 */
	public BooleanProperty allowDetachProperty() {
		if (this.allowDetach == null) {
			this.allowDetach = new SimpleBooleanProperty(this, "allowDetach"); //$NON-NLS-1$
		}
		return this.allowDetach;
	}

	/**
	 * @return whether Tabs can be detached or not
	 */
	public boolean isAllowDetach() {
		return this.allowDetach != null ? this.allowDetach.get() : false;
	}

	/**
	 * @param allowDetach
	 *        whether Tabs can be detached or not
	 */
	public void setAllowDetach(boolean allowDetach) {
		allowDetachProperty().set(allowDetach);
	}

	/**
	 * @return the type of visual Feedback for the User
	 */
	public ObjectProperty<FeedbackType> feedbackTypeProperty() {
		if (this.feedbackType == null) {
			this.feedbackType = new SimpleObjectProperty<>(this, "feedbackType", DEFAULT_FEEDBACK_TYPE); //$NON-NLS-1$
		}
		return this.feedbackType;
	}

	/**
	 * @return the type of visual Feedback for the User
	 */
	public FeedbackType getFeedbackType() {
		return this.feedbackType != null ? this.feedbackType.get() : DEFAULT_FEEDBACK_TYPE;
	}

	/**
	 * @param feedbackType
	 *        the type of visual Feedback for the User
	 */
	public void setFeedbackType(FeedbackType feedbackType) {
		feedbackTypeProperty().set(feedbackType);
	}

}
