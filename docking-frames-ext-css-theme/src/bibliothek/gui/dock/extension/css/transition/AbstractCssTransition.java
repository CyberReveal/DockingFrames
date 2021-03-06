/*
 * Bibliothek - DockingFrames
 * Library built on Java/Swing, allows the user to "drag and drop"
 * panels containing any Swing-Component the developer likes to add.
 * 
 * Copyright (C) 2012 Benjamin Sigg
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Benjamin Sigg
 * benjamin_sigg@gmx.ch
 * CH - Switzerland
 */
package bibliothek.gui.dock.extension.css.transition;

import java.util.HashMap;
import java.util.Map;

import bibliothek.gui.dock.extension.css.CssProperty;
import bibliothek.gui.dock.extension.css.CssPropertyKey;
import bibliothek.gui.dock.extension.css.CssRule;
import bibliothek.gui.dock.extension.css.CssRuleListener;
import bibliothek.gui.dock.extension.css.CssScheme;
import bibliothek.gui.dock.extension.css.CssType;
import bibliothek.gui.dock.extension.css.property.AbstractCssPropertyContainer;
import bibliothek.gui.dock.extension.css.property.IntegerCssProperty;
import bibliothek.gui.dock.extension.css.transition.scheduler.CssSchedulable;
import bibliothek.gui.dock.extension.css.transition.scheduler.CssScheduler;
import bibliothek.util.Filter;

/**
 * This  blends one type of item slowly into another type of item. The  can observe one or many
 * properties of type <code>T</code>. If these properties have any sub-properties, then sub-s can be used
 * to modify them as well.
 * @author Benjamin Sigg
 * @param <T> the type of item this  handles
 */
public abstract class AbstractCssTransition<T> extends AbstractCssPropertyContainer implements CssTransition<T>{
	private CssTransitionCallback callback;
	private CssRule source;
	private CssRule target;
	private Filter<CssPropertyKey> propertyFilter;
	
	private int duration = 500;
	private int time = 0;
	
	/** the list of properties which either are animated or required for the  */
	private Map<CssPropertyKey, AnimatedProperty<?>> properties = new HashMap<CssPropertyKey, AnimatedProperty<?>>();
	
	private CssType<T> type;
	
	private CssPropertyKey durationKey;
	private IntegerCssProperty durationProperty = new IntegerCssProperty(){
		@Override
		public void set( Integer value ){
			if( value == null ){
				duration = 500;
			}
			else{
				duration = value;
			}
		}
		
		@Override
		public void setScheme( CssScheme scheme, CssPropertyKey key ){
			durationKey = key;
		}
	};
	
	private CssRuleListener listener = new CssRuleListener(){
		@Override
		public void selectorChanged( CssRule source ){
			// ignore
		}
		
		@Override
		public void propertiesChanged( CssRule source ){
			for( AnimatedProperty<?> property : properties.values() ){
				property.updateValues();
			}
		}
		
		@Override
		public void propertyChanged( CssRule source, CssPropertyKey key ){
			AnimatedProperty<?> animatedProperty = properties.get( key );
			if( animatedProperty != null ){
				animatedProperty.updateValues();
			}
		}
	};
	
	@Override
	public void setType( CssType<T> type ){
		if( type == null ){
			throw new IllegalArgumentException( "type must not be null" );
		}
		this.type = type;
	}
	
	@Override
	public boolean isInput( CssPropertyKey key ){
		if( key.equals( durationKey ) ){
			return true;
		}
		for( AnimatedProperty<?> value : properties.values() ){
			if( value.dependencies.containsKey( key )){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String[] getPropertyKeys(){
		return new String[]{ "duration" };
	}

	@Override
	public CssProperty<?> getProperty( String key ){
		if( "duration".equals( key )){
			return durationProperty;
		}
		return null;
	}
	
	@Override
	public void setPropertyFilter( Filter<CssPropertyKey> propertyFilter ){
		this.propertyFilter = propertyFilter;
	}
	
	@Override
	public void init( CssRule current, CssTransitionCallback callback ){
		this.callback = callback;
		this.source = current;
		source.addRuleListener( listener );
		CssPropertyKey[] keys = callback.getPropertiesOfType( type );
		for( CssPropertyKey key : keys ){
			if( propertyFilter == null || propertyFilter.includes( key )){
				AnimatedProperty<T> property = new AnimatedProperty<T>( key, createProperty( type, key ), type );
				properties.put( key, property );
				property.updateValues();
			}
		}
	}
	
	@Override
	public void step( int delay ){
		if( delay != -1 ){
			time += delay;
		}
		if( time > duration ){
			for( AnimatedProperty<?> next : properties.values()){
				next.end();
			}
		}
		else{
			double progress = time / (double)duration;
			for( AnimatedProperty<?> next : properties.values()){
				next.updateProgress( progress );
			}
			callback.step();
		}
	}
	
	@Override
	public void transition( CssRule destination ){
		target = destination;
		
		target.addRuleListener( listener );
		
		for( AnimatedProperty<?> property : properties.values() ){
			property.updateValues();
		}
		
		callback.step();
	}
	
	/**
	 * Creates a new animated property.
	 * @param type the type of the property
	 * @param key the name of the property
	 * @return the , not <code>null</code>
	 */
	protected abstract TransitionalCssProperty<T> createProperty( CssType<T> type, CssPropertyKey key );
	
	/**
	 * A wrapper around an {@link TransitionalCssProperty}, knows which other properties are required
	 * for the {@link TransitionalCssProperty} (the dependencies), and automatically updates the
	 * list of dependencies by creating sub-{@link AnimatedProperty}s.
	 * @author Benjamin Sigg
	 * @param <S> the kind of object handled by the wrapped {@link TransitionalCssProperty} 
	 */
	private class AnimatedProperty<S> implements TransitionalCssPropertyCallback<S>, CssSchedulable{
		private TransitionalCssProperty<S> property;
		
		private CssPropertyKey key;
		private CssType<S> type;
		
		private Map<CssPropertyKey, CssProperty<?>> dependencies = new HashMap<CssPropertyKey,CssProperty<?>>();
		
		public AnimatedProperty( CssPropertyKey key, TransitionalCssProperty<S> property, CssType<S> type ){
			this.key = key;
			this.property = property;
			this.type = type;
			property.setCallback( this );
		}
		
		@Override
		public void addSourceDependency( String key, CssProperty<?> property ){
			callback.addSourceDependency( key, property );
			dependencies.put( this.key.append( key ), property );
		}
		
		@Override
		public void removeSourceDependency( String key ){
			dependencies.remove( this.key.append( key ) );
			callback.removeSourceDependency( key );
		}
		
		@Override
		public void addTargetDependency( String key, CssProperty<?> property ){
			callback.addTargetDependency( key, property );	
		}
		
		@Override
		public void removeTargetDependency( String key ){
			callback.removeTargetDependency( key );	
		}
		
		@Override
		public CssScheme getScheme(){
			return callback.getScheme();
		}
		
		public void end(){
			set( target.getProperty( type, key ));
		}
		
		public void updateValues(){
			property.setSource( source.getProperty( type, key ) );
			if( target != null ){
				property.setTarget( target.getProperty( type, key ) );
			}
		}
		
		public void updateProgress( double progress ){
			property.setTransition( progress );
		}

		@Override
		public void set( S value ){
			callback.setProperty( type, key, value );
		}

		@Override
		public void step( CssScheduler scheduler, int delay ){
			property.step( delay );
		}
		
		@Override
		public void step(){
			getScheme().getScheduler().step( this );
		}

		@Override
		public void step( int delay ){
			getScheme().getScheduler().step( this, delay );
		}
	}
}
