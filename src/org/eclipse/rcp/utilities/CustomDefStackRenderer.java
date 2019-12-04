/*******************************************************************************
 * Copyright (c) 2012 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl<tom.schindl@bestsolution.at> - initial API and implementation
 *******************************************************************************/
package org.eclipse.rcp.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.menu.MPopupMenu;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.emf.common.util.URI;
import org.eclipse.fx.core.bindings.FXBindings;
import org.eclipse.fx.ui.controls.Util;
import org.eclipse.fx.ui.services.resources.GraphicsLoader;
import org.eclipse.fx.ui.workbench.fx.EMFUri;
import org.eclipse.fx.ui.workbench.renderers.base.BaseRenderer;
import org.eclipse.fx.ui.workbench.renderers.base.services.DnDFeedbackService;
import org.eclipse.fx.ui.workbench.renderers.base.services.DnDService;
import org.eclipse.fx.ui.workbench.renderers.base.widget.WCallback;
import org.eclipse.fx.ui.workbench.renderers.base.widget.WPopupMenu;
import org.eclipse.fx.ui.workbench.renderers.base.widget.WStack;
import org.eclipse.fx.ui.workbench.renderers.base.widget.WStack.WStackItem;
import org.eclipse.fx.ui.workbench.renderers.fx.DefStackRenderer;
import org.eclipse.fx.ui.workbench.renderers.fx.internal.DnDSupport;
import org.eclipse.fx.ui.workbench.renderers.fx.widget.WLayoutedWidgetImpl;
import org.eclipse.fx.ui.workbench.services.ModelService;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Strings;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Custom base renderer for {@link MPartStack}. It is a copy of {@link DefStackRenderer}, using
 * a different (custom) {@link TabPane} control
 */
@SuppressWarnings("restriction") // org.eclipse.fx.ui.workbench.renderers.base is internal but since we copy a class that depend on it, we don't really have a choice
public abstract class CustomDefStackRenderer extends DefStackRenderer {

	protected abstract Class<? extends WStack<Node, Object, Node>> getStackWidgetClass();

	@Override
	protected Class<? extends WStack<Node, Object, Node>> getWidgetClass(MPartStack stack) {
		if (stack.getTags().contains(WStack.TAG_PAGINATION) || stack.getTags().contains(WStack.TAG_STACKPANE)) {
			return super.getWidgetClass(stack);
		} else {
			return getStackWidgetClass();
		}
	}

	/**
	 * @noreference
	 */
	public static abstract class CustomStackWidgetImpl extends WLayoutedWidgetImpl<Node, Node, MPartStack> implements WStack<Node, Object, Node> {

		WCallback<WStackItem<Object, Node>, Void> mouseSelectedItemCallback;
		WCallback<WStackItem<Object, Node>, Void> keySelectedItemCallback;
		WCallback<@NonNull DragData, @NonNull Boolean> dragStartCallback;

		protected WCallback<WMinMaxState, Void> minMaxCallback;
		// private MinMaxGroup minMaxGroup;
		boolean inKeyTraversal;

		// @Inject
		// private EModelService modelService;

		@Inject
		@NonNull
		DnDFeedbackService dndFeedback;

		@Inject
		@Optional
		@Nullable
		DnDService dndService;

		@NonNull
		@Inject
		ModelService modelService;

		@Inject
		GraphicsLoader graphicsLoader;

		@NonNull
		private final MPartStack domainElement;
		private BorderPane pane;
		private Label titleLabel;

		@Inject
		public CustomStackWidgetImpl(@NonNull @Named(BaseRenderer.CONTEXT_DOM_ELEMENT) MPartStack domainElement) {
			this.domainElement = domainElement;
		}

		@Override
		protected Pane createStaticPane() {
			return new StackPane() {
				@Override
				protected void layoutChildren() {
					super.layoutChildren();
					// if( minMaxGroup != null ) {
					//
					// }
				}
			};
		}

		@Override
		public void setMouseSelectedItemCallback(WCallback<WStackItem<Object, Node>, Void> mouseSelectedItemCallback) {
			this.mouseSelectedItemCallback = mouseSelectedItemCallback;
		}

		@Override
		public void setKeySelectedItemCallback(WCallback<WStackItem<Object, Node>, Void> keySelectedItemCallback) {
			this.keySelectedItemCallback = keySelectedItemCallback;
		}

		@Override
		public AbstractTabPane getWidget() {
			return (AbstractTabPane) super.getWidget();
		}

		@Override
		public int indexOf(WStackItem<Object, Node> item) {
			return getWidget().getTabs().indexOf(item.getNativeItem());
		}

		@Override
		public int getItemCount() {
			return getWidget().getTabs().size();
		}

		@Override
		public void setMinMaxCallback(WCallback<WMinMaxState, Void> minMaxCallback) {
			this.minMaxCallback = minMaxCallback;
		}

		@Override
		public void setMinMaxState(WMinMaxState state) {
			// if( state == WMinMaxState.NONE ) {
			// if( minMaxGroup != null ) {
			// ((Pane)getStaticLayoutNode()).getChildren().remove(minMaxGroup);
			// minMaxGroup = null;
			// }
			// } else {
			// if( minMaxGroup == null ) {
			// minMaxGroup = new MinMaxGroup();
			// minMaxGroup.setManaged(false);
			// minMaxGroup.setState(state);
			// ((Pane)getStaticLayoutNode()).getChildren().add(minMaxGroup);
			// } else {
			// minMaxGroup.setState(state);
			// ((Pane)getStaticLayoutNode()).requestLayout();
			// }
			// }
		}

		protected abstract AbstractTabPane createAbstractTabPane();

		@Override
		protected AbstractTabPane createWidget() {
			DnDSupport dnd = new DnDSupport(
					(param) -> this.dragStartCallback,
					(param) -> this.getDropDroppedCallback(),
					this.dndFeedback,
					this.domainElement, this.dndService, this.modelService);

			AbstractTabPane p = CustomDndTabPaneFactory.createDndTabPane(() -> createAbstractTabPane(), (s) -> {
				s.setStartFunction(dnd::handleDragStart);
				s.setDropConsumer(dnd::handleDropped);
				s.setFeedbackConsumer(dnd::handleFeedback);
				s.setDragFinishedConsumer(dnd::handleFinished);
				s.setClipboardDataFunction(dnd::clipboardDataFunction);
			}, DnDSupport.DETACHABLE_DRAG);

			String location = this.domainElement.getPersistedState().get(WStack.STATE_KEY_TABS_LOCATION);
			if (location != null) {
				Side v = Side.valueOf(location);
				if (v != null) {
					p.setSide(v);
				}
			}

			p.setOnMouseClicked(e -> {
				if (e.getClickCount() > 1) {
					if (this.minMaxCallback != null) {
						p.getChildrenUnmodifiable().stream().filter(n -> n.getStyleClass().contains("tab-header-area")).findFirst().ifPresent( //$NON-NLS-1$
								node -> {
									if (node.localToScene(node.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
										this.minMaxCallback.call(WMinMaxState.TOGGLE);
									}
								});
					}
				}
			});

			if (this.domainElement.getTags().contains(WStack.TAG_TAB_CLOSING_POLICY_ALL_TABS)) {
				p.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
			}
			if (this.domainElement.getTags().contains(WStack.TAG_TAB_CLOSING_POLICY_UNAVAILABLE)) {
				p.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
			}
			if (this.domainElement.getTags().contains(WStack.TAG_TAB_CLOSING_POLICY_SELECTED_TAB)) {
				p.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
			}

			p.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

				@Override
				public void handle(KeyEvent event) {
					CustomStackWidgetImpl.this.inKeyTraversal = true;
				}

			});
			p.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

				@Override
				public void handle(KeyEvent event) {
					CustomStackWidgetImpl.this.inKeyTraversal = false;
				}

			});

			p.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<AbstractTab>() {
				boolean inUpdate;

				@Override
				public void changed(ObservableValue<? extends AbstractTab> observable, AbstractTab oldValue, AbstractTab newValue) {
					if (CustomStackWidgetImpl.this.titleLabel != null) {
						CustomStackWidgetImpl.this.titleLabel.textProperty().unbind();
						CustomStackWidgetImpl.this.titleLabel.graphicProperty().unbind();

						CustomStackWidgetImpl.this.titleLabel.setGraphic(null);
						CustomStackWidgetImpl.this.titleLabel.setText(null);

						if (newValue != null) {
							CustomStackItemImpl d = (CustomStackItemImpl) newValue.getUserData();
							CustomStackWidgetImpl.this.titleLabel.graphicProperty()
							.bind(FXBindings.map(d.iconUri, (u) -> CustomStackWidgetImpl.this.graphicsLoader.getGraphicsNode(u)));
							CustomStackWidgetImpl.this.titleLabel.textProperty().bind(newValue.textProperty());
						}
					}

					if (newValue == null || (getWidgetState() != WidgetState.CREATED && getWidgetState() != WidgetState.IN_SETUP)) {
						return;
					}
					final CustomStackItemImpl w = (CustomStackItemImpl) newValue.getUserData();
					w.handleSelection();

					final WCallback<WStackItem<Object, Node>, Void> cb;
					if (!CustomStackWidgetImpl.this.inKeyTraversal) {
						cb = CustomStackWidgetImpl.this.mouseSelectedItemCallback;
					} else {
						cb = CustomStackWidgetImpl.this.keySelectedItemCallback;
					}

					if (cb != null) {
						if (w.tab.getContent() != null && !w.tab.getContent().isVisible()) {
							// At the moment the visibility changes the content
							// is not yet
							// made visible
							w.tab.getContent().visibleProperty().addListener(new ChangeListener<Boolean>() {

								@Override
								public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
									w.tab.getContent().visibleProperty().removeListener(this);
									if (newValue.booleanValue()) {
										cb.call(w);
									}
								}
							});
						} else {
							if (w.tab.getContent() == null || w.tab.getContent().getScene() != null) {
								// Delay if the subcontrol just got created
								// isVisible() reports true while it is not
								// really
								// See 447924 why we need this inUpdate safe guard
								if (!this.inUpdate) {
									this.inUpdate = true;
									Platform.runLater(new Runnable() {
										@Override
										public void run() {
											cb.call(w);
											inUpdate = false;
										}
									});
								} else {
									CustomStackWidgetImpl.this.logger.debug("An endless activation update has been blocked!"); //$NON-NLS-1$
								}
							} else {
								// We are in the init phase
								cb.call(w);
							}
						}
					}
				}
			});
			return p;
		}

		@Override
		public Node getWidgetNode() {
			if (this.pane == null) {
				this.pane = new BorderPane();

				if (this.domainElement.getTags().contains(WStack.SHOW_TOP_TRIM_AREA_TAG)) {
					HBox box = new HBox();
					box.getStyleClass().add("tool-bar"); //$NON-NLS-1$
					this.titleLabel = new Label();
					box.getChildren().add(this.titleLabel);
					this.pane.setTop(box);
				}

				Node widget = getWidget();

				if (this.domainElement.getTags().contains(BaseRenderer.SCROLLABLE)) {
					ScrollPane scroll = new ScrollPane(widget);
					scroll.setFitToWidth(true);
					scroll.setFitToHeight(true);
					widget = scroll;
				}

				this.pane.setCenter(widget);
			}

			return this.pane;
		}

		@Override
		public void addItem(WStackItem<Object, Node> item) {
			addItems(Collections.singletonList(item));
		}

		@Override
		public void addItems(List<@NonNull WStackItem<Object, Node>> items) {
			List<AbstractTab> extractTabs = extractTabs(items);
			List<Node> nodes = extractTabs.stream()
					.map(AbstractTab::getContent)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			try {
				nodes.forEach(Util::disableStyle);
				getWidget().getTabs().addAll(extractTabs);
			} finally {
				nodes.forEach(Util::restoreStyle);
			}
		}

		@Override
		public void addItems(int index, List<@NonNull WStackItem<Object, Node>> items) {
			if (index >= getWidget().getTabs().size()) {
				addItems(items);
			} else {
				List<AbstractTab> extractTabs = extractTabs(items);
				List<Node> nodes = extractTabs.stream()
						.map(AbstractTab::getContent)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				try {
					nodes.forEach(Util::disableStyle);
					getWidget().getTabs().addAll(index, extractTabs);
				} finally {
					nodes.forEach(Util::restoreStyle);
				}
			}
		}

		private List<AbstractTab> extractTabs(List<WStackItem<Object, Node>> items) {
			List<AbstractTab> tabs = new ArrayList<>(items.size());
			for (WStackItem<Object, Node> t : items) {
				tabs.add((AbstractTab) t.getNativeItem());
			}
			return tabs;
		}

		@Override
		public void selectItem(int idx) {
			getWidget().getSelectionModel().select(idx);
		}

		@Override
		public List<@NonNull WStackItem<Object, Node>> getItems() {
			List<WStackItem<Object, Node>> rv = new ArrayList<>();
			for (AbstractTab t : getWidget().getTabs()) {
				@SuppressWarnings("unchecked")
				WStackItem<Object, Node> i = (WStackItem<Object, Node>) t.getUserData();
				if (i != null) {
					rv.add(i);
				}
			}
			return Collections.unmodifiableList(rv);
		}

		@Override
		public void removeItems(List<WStackItem<Object, Node>> items) {
			List<Object> l = new ArrayList<>();
			for (WStackItem<Object, Node> i : items) {
				l.add(i.getNativeItem());
			}
			getWidget().getTabs().removeAll(l);
		}

		@Override
		public void setDragStartCallback(@NonNull WCallback<@NonNull DragData, @NonNull Boolean> dragStackCallback) {
			this.dragStartCallback = dragStackCallback;
		}
	}

	/**
	 * @noreference
	 */
	public static abstract class CustomStackItemImpl implements WStackItem<Object, Node> {
		AbstractTab tab;
		private WCallback<WStackItem<Object, Node>, Node> initCallback;
		WCallback<WStackItem<Object, Node>, Boolean> closeCallback;

		private MStackElement domElement;

		@Inject
		private GraphicsLoader graphicsLoader;

		@Inject
		private EModelService modelService;

		@Inject
		IPresentationEngine engine;

		private String label;
		private boolean dirty;

		private ObjectProperty<org.eclipse.fx.core.URI> iconUri = new SimpleObjectProperty<>(this, "iconUri", null); //$NON-NLS-1$

		@Inject
		public CustomStackItemImpl(@Named(UIEvents.UILabel.LOCALIZED_LABEL) @Optional String label, @Named(UIEvents.Dirtyable.DIRTY) @Optional boolean dirty) {
			this.label = label;
			this.dirty = dirty;
		}

		@PostConstruct
		void init() {
			getWidget();
		}

		@Override
		@SuppressWarnings("unchecked")
		public void setDomElement(MStackElement domElement) {
			this.domElement = domElement;
			if (this.domElement instanceof MPart) {
				((MPart) this.domElement)
				.getMenus()
				.stream()
				.filter(m -> m instanceof MPopupMenu)
				.filter(m -> m.getTags().contains(TAG_TAB_CONTEXT_MENU))
				.findFirst()
				.map(m -> {
					return (ContextMenu) ((WPopupMenu<ContextMenu>) this.engine.createGui(m, null,
							modelService.getContainingContext(domElement.getParent()))).getWidget();
				})
				.ifPresent(getWidget()::setContextMenu);
			} else {
				getWidget().setContextMenu(null);
			}
		}

		@Override
		public MStackElement getDomElement() {
			return this.domElement;
		}

		protected AbstractTab getWidget() {
			if (this.tab == null) {
				this.tab = createWidget();
			}
			this.tab.setUserData(this);
			return this.tab;
		}

		protected AbstractTab createWidget() {
			final AbstractTab t = createAbstractTab();
			t.setOnCloseRequest(this::handleOnCloseRequest);
			return t;
		}

		protected abstract AbstractTab createAbstractTab();

		private void handleOnCloseRequest(Event event) {
			if (this.closeCallback.call(this).booleanValue()) {
				event.consume();
			}
		}

		void handleSelection() {
			if (this.initCallback != null) {
				Node node = this.initCallback.call(this);
				Util.attachNode(node, this.tab::setContent);
				this.initCallback = null;
			}

			if (WLayoutedWidgetImpl.OPTIMIZED_STACK_LAYOUT) {
				if (this.tab.getContent() instanceof Parent) {
					this.domElement.getParent().setSelectedElement(this.domElement);
					((Parent) this.tab.getContent()).requestLayout();
				} else {
					this.tab.getTabPane().requestLayout();
				}
			}
		}

		@Override
		public void setInitCallback(WCallback<WStackItem<Object, Node>, Node> initCallback) {
			this.initCallback = initCallback;
		}

		@Override
		public AbstractTab getNativeItem() {
			return getWidget();
		}

		@Inject
		public void setLabel(@Named(UIEvents.UILabel.LOCALIZED_LABEL) @Optional String label) {
			this.label = label;
			getWidget().setText(this.dirty ? "*" + notNull(label) : notNull(label)); //$NON-NLS-1$
		}

		@Inject
		public void setTooltip(@Named(UIEvents.UILabel.LOCALIZED_TOOLTIP) @Optional String tooltip) {
			if (tooltip != null && !tooltip.isEmpty()) {
				getWidget().setTooltip(new Tooltip(tooltip));
			} else {
				getWidget().setTooltip(null);
			}
		}

		@Inject
		public void setCloseable(@Named(UIEvents.Part.CLOSEABLE) @Optional boolean closeable) {
			getWidget().setClosable(closeable);
		}

		@Inject
		public void setIcon(@Named(UIEvents.UILabel.ICONURI) @Optional String iconUri) {
			if (!Strings.isNullOrEmpty(iconUri)) {
				EMFUri uri = new EMFUri(URI.createURI(iconUri));
				this.iconUri.set(uri);
				getWidget().setGraphic(this.graphicsLoader.getGraphicsNode(uri));
			} else {
				this.iconUri.set(null);
				getWidget().setGraphic(null);
			}
		}

		@Inject
		public void setDirty(@Named(UIEvents.Dirtyable.DIRTY) @Optional boolean dirty) {
			this.dirty = dirty;
			getWidget().setText(dirty ? "*" + notNull(this.label) : notNull(this.label)); //$NON-NLS-1$
		}

		@Override
		public void setOnCloseCallback(final WCallback<WStackItem<Object, Node>, Boolean> callback) {
			this.closeCallback = callback;
		}

		private String notNull(String s) {
			return s == null ? "" : s; //$NON-NLS-1$
		}

		@PreDestroy
		public void dispose() {
			this.initCallback = null;
			this.closeCallback = null;
			this.graphicsLoader = null;
			this.tab.setOnCloseRequest(null);
			this.tab = null;
		}
	}

}
