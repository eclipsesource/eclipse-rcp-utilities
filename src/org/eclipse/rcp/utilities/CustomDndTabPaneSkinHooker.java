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
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DropType;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.DroppedData;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.FeedbackData;
import org.eclipse.fx.ui.controls.tabpane.GenericTab;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Create a hooker who use native DnD
 */
public class CustomDndTabPaneSkinHooker implements CustomDragSetup {
	private static AbstractTab DRAGGED_TAB;
	/**
	 * Custom data format for move data
	 */
	public static final DataFormat TAB_MOVE = new DataFormat("DnDTabPane:tabMove"); //$NON-NLS-1$

	private @Nullable Function<@NonNull GenericTab, @NonNull Boolean> startFunction;
	private @Nullable Function<@NonNull GenericTab, @NonNull String> clipboardDataFunction;
	private @Nullable Consumer<@NonNull GenericTab> dragFinishedConsumer;
	private @Nullable Consumer<@NonNull FeedbackData> feedbackConsumer;
	private @Nullable Consumer<@NonNull DroppedData> dropConsumer;

	private final AbstractTabPane pane;

	private Runnable cleanup;

	/**
	 * Create a new hooker instance
	 *
	 * @param skin
	 *            the skin
	 */
	public CustomDndTabPaneSkinHooker(AbstractTabPane tabPane) {
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

		// Hook the nodes
		tabHeaderArea.setOnDragOver((e) -> e.consume());

		EventHandler<MouseEvent> handlerDragDetect = this::tabPane_handleDragStart;
		EventHandler<DragEvent> handlerFinished = this::tabPane_handleDragDone;
		EventHandler<? super DragEvent> handlerDragOver = (e) -> tabPane_handleDragOver(tabHeaderArea, headersRegion, e);
		EventHandler<? super DragEvent> handlerDropped = (e) -> tabPane_handleDragDropped(tabHeaderArea, headersRegion, e);
		EventHandler<? super DragEvent> handleDragDone = this::tabPane_handleDragDone;

		for (Node tabHeaderSkin : headersRegion.getChildren()) {
			tabHeaderSkin.addEventHandler(MouseEvent.DRAG_DETECTED, handlerDragDetect);
			tabHeaderSkin.addEventHandler(DragEvent.DRAG_DONE, handlerFinished);
		}

		ListChangeListener<Node> nodeChangeListener = (javafx.collections.ListChangeListener.Change<? extends Node> change) -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					change.getRemoved().forEach((e) -> e.removeEventHandler(MouseEvent.DRAG_DETECTED, handlerDragDetect));
					change.getRemoved().forEach((e) -> e.removeEventHandler(DragEvent.DRAG_DONE, handlerFinished));
				}
				if (change.wasAdded()) {
					change.getAddedSubList().forEach((e) -> e.addEventHandler(MouseEvent.DRAG_DETECTED, handlerDragDetect));
					change.getAddedSubList().forEach((e) -> e.addEventHandler(DragEvent.DRAG_DONE, handlerFinished));
				}
			}
		};
		headersRegion.getChildren().addListener(nodeChangeListener);

		tabHeaderArea.addEventHandler(DragEvent.DRAG_OVER, handlerDragOver);
		tabHeaderArea.addEventHandler(DragEvent.DRAG_DROPPED, handlerDropped);
		tabHeaderArea.addEventHandler(DragEvent.DRAG_EXITED, handleDragDone);

		cleanup = () -> {
			for (Node tabHeaderSkin : headersRegion.getChildren()) {
				tabHeaderSkin.removeEventHandler(MouseEvent.DRAG_DETECTED, handlerDragDetect);
				tabHeaderSkin.removeEventHandler(DragEvent.DRAG_DONE, handlerFinished);
			}

			headersRegion.getChildren().removeListener(nodeChangeListener);
			tabHeaderArea.removeEventHandler(DragEvent.DRAG_OVER, handlerDragOver);
			tabHeaderArea.removeEventHandler(DragEvent.DRAG_DROPPED, handlerDropped);
			tabHeaderArea.removeEventHandler(DragEvent.DRAG_EXITED, handleDragDone);
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

	@SuppressWarnings("all")
	void tabPane_handleDragDropped(Pane tabHeaderArea, Pane headersRegion, DragEvent event) {
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
					event.setDropCompleted(false);
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
					event.setDropCompleted(true);
				} else {
					event.setDropCompleted(false);
				}
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			event.consume();
		}
	}

	void tabPane_handleDragStart(MouseEvent event) {
		try {
			AbstractTab t = getTab((Node) event.getSource());

			if (t != null && efx_canStartDrag(CustomTabWrapper.wrap(t))) {
				DRAGGED_TAB = t;
				Node node = (Node) event.getSource();
				Dragboard db = node.startDragAndDrop(TransferMode.MOVE);

				WritableImage snapShot = node.snapshot(new SnapshotParameters(), null);
				PixelReader reader = snapShot.getPixelReader();
				int padX = 10;
				int padY = 10;
				int width = (int) snapShot.getWidth();
				int height = (int) snapShot.getHeight();
				WritableImage image = new WritableImage(width + padX, height + padY);
				PixelWriter writer = image.getPixelWriter();

				int h = 0;
				int v = 0;
				while (h < width + padX) {
					v = 0;
					while (v < height + padY) {
						if (h >= padX && h <= width + padX && v >= padY && v <= height + padY) {
							writer.setColor(h, v, reader.getColor(h - padX, v - padY));
						} else {
							writer.setColor(h, v, Color.TRANSPARENT);
						}

						v++;
					}
					h++;
				}

				db.setDragView(image, image.getWidth(), image.getHeight() * -1);

				ClipboardContent content = new ClipboardContent();
				String data = efx_getClipboardContent(CustomTabWrapper.wrap(t));
				if (data != null) {
					content.put(TAB_MOVE, data);
				}
				db.setContent(content);
			}
		} catch (Throwable t) {
			// // TODO Auto-generated catch block
			t.printStackTrace();
		}
	}

	@SuppressWarnings("all")
	void tabPane_handleDragOver(Pane tabHeaderArea, Pane headersRegion, DragEvent event) {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			event.acceptTransferModes(TransferMode.MOVE);
		} else {
			efx_dragFeedback(CustomTabWrapper.wrap(draggedTab), null, null, DropType.NONE);
		}
	}

	void tabPane_handleDragDone(DragEvent event) {
		AbstractTab tab = DRAGGED_TAB;
		if (tab == null) {
			return;
		}

		efx_dragFinished(CustomTabWrapper.wrap(tab));
	}

	private boolean efx_canStartDrag(@NonNull GenericTab tab) {
		if (this.startFunction != null) {
			return this.startFunction.apply(tab).booleanValue();
		}
		return true;
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

	private void efx_dragFeedback(@NonNull GenericTab draggedTab, GenericTab targetTab, Bounds bounds, @NonNull DropType dropType) {
		if (this.feedbackConsumer != null) {
			this.feedbackConsumer.accept(new FeedbackData(draggedTab, targetTab, bounds, dropType));
		}
	}

	private void efx_dropped(double x, double y, @NonNull GenericTab draggedTab, @NonNull GenericTab targetTab, @NonNull DropType dropType) {
		if (this.dropConsumer != null) {
			this.dropConsumer.accept(new DroppedData(x, y, draggedTab, targetTab, dropType));
		}
	}
}
