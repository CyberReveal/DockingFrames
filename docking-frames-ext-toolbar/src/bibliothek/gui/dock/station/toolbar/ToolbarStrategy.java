package bibliothek.gui.dock.station.toolbar;

import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.util.PropertyKey;
import bibliothek.gui.dock.util.property.ConstantPropertyFactory;

/**
 * A {@link ToolbarStrategy} defines how different parts of a toolbar interact with each other. 
 * @author Benjamin Sigg
 */
public interface ToolbarStrategy {
	/**
	 * {@link PropertyKey} for the current {@link ToolbarStrategy} to use.
	 */
	public static final PropertyKey<ToolbarStrategy> STRATEGY = new PropertyKey<ToolbarStrategy>( "dock.toolbarStrategy",
			new ConstantPropertyFactory<ToolbarStrategy>( new DefaultToolbarStrategy() ), true );


	/**
	 * As toolbars have constraints on which {@link Dockable} can be a child of which {@link DockStation} often additional
	 * layers between a specific {@link DockStation} and a {@link Dockable} are required. This method defines what
	 * these layers are. 
	 * @param parent some {@link DockStation} which is going to become a direct or indirect parent of <code>child</code>
	 * @param child some {@link Dockable} that is going to be a direct or indirect child of <code>parent</code>
	 * @return the element that is actually added to <code>parent</code> as direct child. This can either be
	 * <code>child</code>, or a new dockable {@link DockStation} containing <code>child</code>. A value of <code>null</code>
	 * indicates that <code>child</code> can never be any kind of child of <code>parent</code>. The other methods
	 * of this {@link ToolbarStrategy} should however be implemented such that this case never happens during a 
	 * drag and drop operation.
	 */
	public Dockable ensureToolbarLayer( DockStation parent, Dockable child );

	/**
	 * Tells whether the station <code>parent</code> is a valid choice for the dockable <code>child</code>, assuming
	 * that child represents a component of a toolbar, e.g. a button.  
	 * @param parent a potential parent for <code>child</code>. It might be that an additional layer between
	 * <code>parent</code> and <code>child</code> will be created.
	 * @param child the potential new child of <code>parent</code>
	 * @param strongRelation if <code>true</code>, then it must be possible to add <code>child</code> directly
	 * to <code>parent</code>, if <code>false</code> the relation must be valid after calling {@link #ensureToolbarLayer(DockStation, Dockable)}. 
	 * @return <code>true</code> if a combination between <code>child</code> and <code>parent</code> is possible
	 */
	public boolean isToolbarGroupPartParent( DockStation parent, Dockable child, boolean strongRelation );

	/**
	 * Tells whether <code>dockable</code> represents a group of toolbar components, e.g. a group
	 * of buttons. Also a single toolbar component can be understood to be a group of components.
	 * @param dockable some dockable that is drag and dropped
	 * @return <code>true</code> if <code>dockable</code> represents a group of toolbar components
	 */
	public boolean isToolbarGroupPart( Dockable dockable );
	
	/**
	 * Tells whether <code>dockable</code> represents a toolbar. A toolbar can either be a single item
	 * like a button, a {@link #isToolbarGroupPart(Dockable) group of such items}, or a real toolbar.
	 * @param dockable the element to test
	 * @return <code>true</code> if <code>dockable</code> represents a toolbar or a part of a toolbar
	 */
	public boolean isToolbarPart( Dockable dockable );
}
