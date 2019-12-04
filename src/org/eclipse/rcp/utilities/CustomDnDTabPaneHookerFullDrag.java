/*******************************************************************************
 * Copyright (c) 2017 BestSolution.at and others.
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
import java.util.function.Function;

import org.eclipse.fx.core.log.LoggerCreator;
import org.eclipse.fx.ui.controls.Util;
import org.eclipse.fx.ui.controls.dnd.EFXDragEvent;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DropType;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DroppedData;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.FeedbackData;
import org.eclipse.fx.ui.controls.tabpane.GenericTab;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import javafx.animation.ScaleTransition;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Hook a CustomTabPane and allow detaching
 */
@SuppressWarnings("restriction")
public class CustomDnDTabPaneHookerFullDrag implements CustomDragSetup {
	private static AbstractTab DRAGGED_TAB;
	private final AbstractTabPane pane;

	private @Nullable Function<@NonNull GenericTab, @NonNull Boolean> startFunction;
	private @Nullable Consumer<@NonNull GenericTab> dragFinishedConsumer;
	private @Nullable Consumer<@NonNull FeedbackData> feedbackConsumer;
	private @Nullable Consumer<@NonNull DroppedData> dropConsumer;
	private @Nullable Function<@NonNull GenericTab, @NonNull String> clipboardDataFunction;

	private Runnable cleanup;

	/**
	 * Create a new hooker
	 *
	 * @param skin
	 *            the skin
	 */
	public CustomDnDTabPaneHookerFullDrag(AbstractTabPane tabPane) {
		this.pane = tabPane;
		Node n_tabHeaderArea = tabPane.lookup(".header");
		if (!(n_tabHeaderArea instanceof Pane)) {
			LoggerCreator.createLogger(CustomDnDTabPaneHookerFullDrag.class).warning("Could not find a supported header pane. DnD is disabled."); //$NON-NLS-1$
			return;
		}

		Pane tabHeaderArea = (Pane) n_tabHeaderArea;

		Node n_headersRegion = tabHeaderArea.lookup(".tabs");

		if (!(n_headersRegion instanceof Pane)) {
			LoggerCreator.createLogger(CustomDnDTabPaneHookerFullDrag.class).warning("Could not find a supported HeadersRegion pane. DnD is disabled."); //$NON-NLS-1$
			return;
		}

		Pane headersRegion = (Pane) n_headersRegion;

		EventHandler<MouseEvent> handle_dragDetect = this::tabPane_handleDragStart;
		EventHandler<EFXDragEvent> handlerFinished = this::tabPane_handleDragDone;
		EventHandler<MouseEvent> handle_mouseDragged = this::handle_mouseDragged;
		EventHandler<MouseEvent> handleMouseReleased = this::handleMouseReleased;
		EventHandler<? super EFXDragEvent> handle_dragOver = (e) -> tabPane_handleDragOver(tabHeaderArea, headersRegion, e);
		EventHandler<? super EFXDragEvent> handle_dragDropped = (e) -> tabPane_handleDragDropped(tabHeaderArea, headersRegion, e);
		EventHandler<? super EFXDragEvent> handle_dragDone = this::tabPane_handleDragDone;

		for (Node tabHeaderSkin : headersRegion.getChildren()) {
			tabHeaderSkin.addEventHandler(MouseEvent.DRAG_DETECTED, handle_dragDetect);
			tabHeaderSkin.addEventHandler(MouseEvent.MOUSE_DRAGGED, handle_mouseDragged);
			tabHeaderSkin.addEventHandler(MouseEvent.MOUSE_RELEASED, handleMouseReleased);
			tabHeaderSkin.addEventHandler(EFXDragEvent.DRAG_DONE, handlerFinished);
		}

		ListChangeListener<Node> nodeChangeListener = (javafx.collections.ListChangeListener.Change<? extends Node> change) -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					change.getRemoved().forEach((e) -> e.removeEventHandler(MouseEvent.DRAG_DETECTED, handle_dragDetect));
					change.getRemoved().forEach((e) -> e.removeEventHandler(MouseEvent.MOUSE_DRAGGED, handle_mouseDragged));
					change.getRemoved().forEach((e) -> e.removeEventHandler(MouseEvent.MOUSE_RELEASED, handleMouseReleased));
					// change.getRemoved().forEach((e) ->
					// e.removeEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED,
					// handlerFinished));
				}
				if (change.wasAdded()) {
					change.getAddedSubList().forEach((e) -> e.addEventHandler(MouseEvent.DRAG_DETECTED, handle_dragDetect));
					change.getAddedSubList().forEach((e) -> e.addEventHandler(MouseEvent.MOUSE_DRAGGED, handle_mouseDragged));
					change.getAddedSubList().forEach((e) -> e.addEventHandler(MouseEvent.MOUSE_RELEASED, handleMouseReleased));
					// change.getAddedSubList().forEach((e) ->
					// e.addEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED,
					// handlerFinished));
				}
			}
		};
		headersRegion.getChildren().addListener(nodeChangeListener);

		// tabHeaderArea.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, (e) ->
		// tabPane_handleDragOver(tabHeaderArea, headersRegion, e));

		tabHeaderArea.addEventHandler(EFXDragEvent.DRAG_OVER, handle_dragOver);
		tabHeaderArea.addEventHandler(EFXDragEvent.DRAG_DROPPED, handle_dragDropped);
		// tabHeaderArea.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED,
		// this::tabPane_handleDragDone);

		this.pane.addEventHandler(EFXDragEvent.DRAG_DONE, handle_dragDone);

		this.cleanup = () -> {
			for (Node tabHeaderSkin : headersRegion.getChildren()) {
				tabHeaderSkin.removeEventHandler(MouseEvent.DRAG_DETECTED, handle_dragDetect);
				tabHeaderSkin.removeEventHandler(MouseEvent.MOUSE_DRAGGED, handle_mouseDragged);
				tabHeaderSkin.removeEventHandler(MouseEvent.MOUSE_RELEASED, handleMouseReleased);
				tabHeaderSkin.removeEventHandler(EFXDragEvent.DRAG_DONE, handlerFinished);
			}

			headersRegion.getChildren().removeListener(nodeChangeListener);
			tabHeaderArea.removeEventHandler(EFXDragEvent.DRAG_OVER, handle_dragOver);
			tabHeaderArea.removeEventHandler(EFXDragEvent.DRAG_DROPPED, handle_dragDropped);
			this.pane.removeEventHandler(EFXDragEvent.DRAG_DONE, handle_dragDone);
		};
	}

	@Override
	public void dispose() {
		cleanup.run();
	}

	@Override
	public void setClipboardDataFunction(@Nullable Function<@NonNull GenericTab, @NonNull String> clipboardDataFunction) {
		this.clipboardDataFunction = clipboardDataFunction;
	}

	@Override
	public void setDragFinishedConsumer(Consumer<@NonNull GenericTab> dragFinishedConsumer) {
		this.dragFinishedConsumer = dragFinishedConsumer;
	}

	@Override
	public void setDropConsumer(Consumer<@NonNull DroppedData> dropConsumer) {
		this.dropConsumer = dropConsumer;
	}

	@Override
	public void setFeedbackConsumer(Consumer<@NonNull FeedbackData> feedbackConsumer) {
		this.feedbackConsumer = feedbackConsumer;
	}

	@Override
	public void setStartFunction(@Nullable Function<@NonNull GenericTab, @NonNull Boolean> startFunction) {
		this.startFunction = startFunction;
	}

	private AbstractTab getTab(Node n) {
		int tabIdx = n.getParent().getChildrenUnmodifiable().indexOf(n); // The
		// order
		// in
		// the
		// parent
		// ==
		// order
		// in
		// pane.getTabs()
		return this.pane.getTabs().get(tabIdx);
	}

	void tabPane_handleDragStart(MouseEvent event) {
		try {
			AbstractTab t = getTab((Node) event.getSource());

			if (t != null && efx_canStartDrag(CustomTabWrapper.wrap(t))) {
				DRAGGED_TAB = t;

				Node n = (Node) event.getSource();
				n.startFullDrag();

				String data = efx_getClipboardContent(CustomTabWrapper.wrap(t));
				EFXDragEvent evt = new EFXDragEvent(event.getSource(), event.getTarget(), EFXDragEvent.DRAG_START, event.getScreenX(), event.getScreenY(), false);
				evt.setDraggedContent(data);
				evt.updateFeedback(p -> {
					final SnapshotParameters snapshotParameters = new SnapshotParameters();
					snapshotParameters.setFill(Color.TRANSPARENT);
					WritableImage snapShot = n.snapshot(snapshotParameters, null);
					ImageView v = new ImageView(snapShot);
					// if (!p.getStyleClass().contains(styleClassPrefix +
					// "-tab-folder-dragimage")){ // I am a hack
					// p.getStyleClass().add(styleClassPrefix +
					// "-tab-folder-dragimage"); // me too
					// }
					ScaleTransition st = new ScaleTransition(Duration.millis(200), v);
					st.setFromX(0);
					st.setToX(1);
					st.play();

					p.getChildren().add(v);
				});
				Event.fireEvent(event.getTarget(), evt);
				event.consume();
			}
		} catch (Throwable t) {
			// // TODO Auto-generated catch block
			t.printStackTrace();
		}
	}

	void tabPane_handleDragDone(EFXDragEvent event) {
		AbstractTab tab = DRAGGED_TAB;
		if (tab == null) {
			return;
		}

		efx_dragFinished(CustomTabWrapper.wrap(tab));
	}

	void handle_mouseDragged(MouseEvent e) {
		if (DRAGGED_TAB == null) {
			return;
		}
		Node node = Util.findNode((Window) null, e.getScreenX(), e.getScreenY());
		if (node != null) {
			Window window = node.getScene().getWindow();
			if (window instanceof Stage) {
				((Stage) window).toFront();
			}
			Event.fireEvent(node, new EFXDragEvent(this, node, EFXDragEvent.DRAG_OVER, e.getScreenX(), e.getScreenY(), false));
		} else {
			EFXDragEvent.updateFeedbackLocation(e.getScreenX(), e.getScreenY());
		}
	}

	@SuppressWarnings("all")
	void tabPane_handleDragOver(Pane tabHeaderArea, Pane headersRegion, EFXDragEvent event) {
		AbstractTab draggedTab = DRAGGED_TAB;
		if (draggedTab == null) {
			return;
		}

		// Consume the drag in any case
		event.consume();

		double x = event.getX() - headersRegion.getBoundsInParent().getMinX();

		Node referenceNode = null;
		DropType type = DropType.AFTER;
		for (Node n : headersRegion.getChildren()) {
			Bounds b = n.getBoundsInParent();
			if (b.getMaxX() > x) {
				if (b.getMinX() + b.getWidth() / 2 > x) {
					referenceNode = n;
					type = DropType.BEFORE;
				} else {
					referenceNode = n;
					type = DropType.AFTER;
				}
				break;
			}
		}

		if (referenceNode == null && headersRegion.getChildren().size() > 0) {
			referenceNode = headersRegion.getChildren().get(headersRegion.getChildren().size() - 1);
			type = DropType.AFTER;
		}

		if (referenceNode != null) {
			try {
				AbstractTab tab = getTab(referenceNode);

				boolean noMove = false;
				if (tab == draggedTab) {
					noMove = true;
				} else if (type == DropType.BEFORE) {
					int idx = pane.getTabs().indexOf(tab);
					if (idx > 0) {
						if (pane.getTabs().get(idx - 1) == draggedTab) {
							noMove = true;
						}
					}
				} else {
					int idx = pane.getTabs().indexOf(tab);

					if (idx + 1 < pane.getTabs().size()) {
						if (pane.getTabs().get(idx + 1) == draggedTab) {
							noMove = true;
						}
					}
				}

				if (noMove) {
					efx_dragFeedback(CustomTabWrapper.wrap(draggedTab), null, null, DropType.NONE);
					return;
				}

				Bounds b = referenceNode.getBoundsInLocal();
				b = referenceNode.localToScene(b);
				b = pane.sceneToLocal(b);

				efx_dragFeedback(CustomTabWrapper.wrap(draggedTab), CustomTabWrapper.wrap(tab), b, type);
			} catch (Throwable e) {
				LoggerCreator.createLogger(getClass()).error("Failure while handling drag over", e);
			}
		} else {
			efx_dragFeedback(CustomTabWrapper.wrap(draggedTab), null, null, DropType.NONE);
		}
	}

	@SuppressWarnings("all")
	void tabPane_handleDragDropped(Pane tabHeaderArea, Pane headersRegion, EFXDragEvent event) {
		AbstractTab draggedTab = DRAGGED_TAB;
		if (draggedTab == null) {
			return;
		}

		double x = event.getX() - headersRegion.getBoundsInParent().getMinX();

		Node referenceNode = null;
		DropType type = DropType.AFTER;
		for (Node n : headersRegion.getChildren()) {
			Bounds b = n.getBoundsInParent();
			if (b.getMaxX() > x) {
				if (b.getMinX() + b.getWidth() / 2 > x) {
					referenceNode = n;
					type = DropType.BEFORE;
				} else {
					referenceNode = n;
					type = DropType.AFTER;
				}
				break;
			}
		}

		if (referenceNode == null && headersRegion.getChildren().size() > 0) {
			referenceNode = headersRegion.getChildren().get(headersRegion.getChildren().size() - 1);
			type = DropType.AFTER;
		}

		if (referenceNode != null) {
			try {
				AbstractTab tab = getTab(referenceNode);

				boolean noMove = false;
				if (tab == null) {
					event.setComplete(false);
					return;
				} else if (tab == draggedTab) {
					noMove = true;
				} else if (type == DropType.BEFORE) {
					int idx = pane.getTabs().indexOf(tab);
					if (idx > 0) {
						if (pane.getTabs().get(idx - 1) == draggedTab) {
							noMove = true;
						}
					}
				} else {
					int idx = pane.getTabs().indexOf(tab);

					if (idx + 1 < pane.getTabs().size()) {
						if (pane.getTabs().get(idx + 1) == draggedTab) {
							noMove = true;
						}
					}
				}

				if (!noMove) {
					efx_dropped(event.getScreenX(), event.getScreenY(), CustomTabWrapper.wrap(draggedTab), CustomTabWrapper.wrap(tab), type);
					event.setComplete(true);
				} else {
					event.setComplete(false);
				}
			} catch (Throwable e) {
				LoggerCreator.createLogger(getClass()).error("Error while handling drop",e);
			}

			event.consume();
		}
	}

	private void handleMouseReleased(MouseEvent e) {
		if (DRAGGED_TAB == null) {
			return;
		}

		boolean isComplete = false;
		try {
			Node node = Util.findNode((Window) null, e.getScreenX(), e.getScreenY());
			if (node != null) {
				EFXDragEvent event = new EFXDragEvent(node, node, EFXDragEvent.DRAG_DROPPED, e.getScreenX(), e.getScreenY(), false);
				Event.fireEvent(node, event);
				isComplete = event.isComplete();
			} else {
				efx_dropped(e.getScreenX(), e.getScreenY(), CustomTabWrapper.wrap(DRAGGED_TAB), null, DropType.DETACH);
			}
		} finally {
			Event.fireEvent(this.pane, new EFXDragEvent(this.pane, this.pane, EFXDragEvent.DRAG_DONE, e.getScreenX(), e.getScreenY(), isComplete));
			DRAGGED_TAB = null;
		}
	}

	private boolean efx_canStartDrag(@NonNull GenericTab tab) {
		if (this.startFunction != null) {
			return this.startFunction.apply(tab).booleanValue();
		}
		return true;
	}

	private void efx_dragFeedback(@NonNull GenericTab draggedTab, GenericTab targetTab, Bounds bounds, @NonNull DropType dropType) {
		if (this.feedbackConsumer != null) {
			this.feedbackConsumer.accept(new FeedbackData(draggedTab, targetTab, bounds, dropType));
		}
	}

	private void efx_dropped(double x, double y, @NonNull GenericTab draggedTab, @Nullable GenericTab targetTab, @NonNull DropType dropType) {
		if (this.dropConsumer != null) {
			this.dropConsumer.accept(new DroppedData(x, y, draggedTab, targetTab, dropType));
		}
	}

	private void efx_dragFinished(@NonNull GenericTab tab) {
		if (this.dragFinishedConsumer != null) {
			this.dragFinishedConsumer.accept(tab);
		}
	}

	private String efx_getClipboardContent(@NonNull GenericTab t) {
		if (this.clipboardDataFunction != null) {
			return this.clipboardDataFunction.apply(t);
		}
		return System.identityHashCode(t) + ""; //$NON-NLS-1$
	}
}
