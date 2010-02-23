/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.we.utils;

import java.util.ArrayList;
import java.util.List;

import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.Section;

/**
 * This class offers some methods for the KnowWETypeBrowser
 * and the KnowWETypeActivator
 * 
 * @author Johannes Dienst
 *
 */
public class KnowWEObjectTypeUtils {

	/**
	 * Removes duplicates.
	 * Needed because the java.util.Set-approach wont work
	 * (Different Objects).
	 * TODO: Maybe implement a special Set for this
	 * 
	 * @param cleanMe
	 * @return
	 */
	public static List<KnowWEObjectType> cleanList(List<KnowWEObjectType> cleanMe) {
		List<KnowWEObjectType> cleaned = new ArrayList<KnowWEObjectType>();
		for(int i = 0;i < cleanMe.size();i++) {
			String name = cleanMe.get(i).getName();
			cleaned.add(cleanMe.get(i));
			for(int j = i+1;j < cleanMe.size();j++) {
				if((cleanMe.get(j).getName()).equals(name)) {
					cleanMe.remove(j--);
				}
			}
		}
		return cleaned;
	}
	
	/**
	 * Get the father element of the current cell content section. Search as long as the section
	 * is instance of AbstractXMLObjectType. Used to get the <code>Table</code> section itself.
	 * 
	 * @param child
	 * @return
	 */
	@Deprecated
	public static Section getAncestorOfType(Section child, String classname) {
		if( child == null )
			return null;
		
		try {
			if( Class.forName( classname ).isAssignableFrom(
					child.getObjectType().getClass() ) ) return child;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		 
		return getAncestorOfType(child.getFather(), classname);
	}
	
	/**
	 * Get the father element of the current cell content section. Search as long as the section
	 * is instance of AbstractXMLObjectType. Used to get the <code>Table</code> section itself.
	 * 
	 * @param child
	 * @return
	 */
	public static <T extends KnowWEObjectType>Section<T> getAncestorOfType(Section child, T t) {
		if( child == null )
			return null;
		
			if( t.getClass().isAssignableFrom(
					child.getObjectType().getClass() ) ) return child;
	
		 
		return getAncestorOfType(child.getFather(), t);
	}
	
	/**
	 * Getting of all ChildrenTypes of a
	 * KnowWEObjectType.
	 * 
	 * @param type
	 * @param allTypes
	 * @return
	 */
	public static KnowWEObjectTypeSet 
			getAllChildrenTypesRecursive(KnowWEObjectType type, KnowWEObjectTypeSet allTypes) {
		
		// Recursionstop
		if (allTypes.contains(type)) {
			return allTypes;
		}
		
		allTypes.add(type);
		
		// check all allowed children types from this type
		if(type.getAllowedChildrenTypes() != null) {
			
			// NOTE: .getAllowedChildrenTypes() now returns an unmodifiable list => copy
			List<KnowWEObjectType> unModList = type.getAllowedChildrenTypes();
			List<KnowWEObjectType> moreChildren = new ArrayList<KnowWEObjectType>();
			moreChildren.addAll(unModList);
			
			// Loop Protection
			if (hasTypeInList(moreChildren, type))
				removeTypeFromList(moreChildren, type);
			
			for (KnowWEObjectType childrentype : moreChildren) {
				
				// if children does not contain this type
				if (!allTypes.contains(childrentype)) {
					allTypes.add(childrentype);
					for (KnowWEObjectType c : childrentype.getAllowedChildrenTypes()) {
						KnowWEObjectTypeSet t = getAllChildrenTypesRecursive(c, allTypes);
						allTypes.addAll(t.toList());
					}
				}
			}
		}
		return allTypes;
	}
	
	/**
	 * Removes a Type from a given List, because list.remove(type) is
	 * not functional (Different Objects)
	 * 
	 * @param types
	 * @param type
	 * @return
	 */
	private static void removeTypeFromList(List<? extends KnowWEObjectType> types, KnowWEObjectType type) {		
		for (int i = 0; i < types.size(); i++) {
			if (types.get(i).getName().equals(type.getName())) {
				types.remove(i--);
			}
		}
	}

	/**
	 * Test if List contains a type.
	 * 
	 * @param children
	 * @param type
	 * @return
	 */
	private static boolean hasTypeInList(List<? extends KnowWEObjectType> children, KnowWEObjectType type) {		
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				if (children.get(i).getName().equals(type.getName())){
					return true;
				}				
			}
			return false;
		}
		return true;
	}

	/**
	 * Get the father element of the given section specified by class.
	 * 
	 * @param child
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static<OT extends KnowWEObjectType> Section<OT> getAncestorOfType(Section<? extends KnowWEObjectType> child, Class<OT> clazz) {
		if( child == null ) 
			return null;		
		
		if( clazz.isAssignableFrom( child.getObjectType().getClass())) 
			return (Section<OT>)child;
		
		return getAncestorOfType(child.getFather(), clazz);
	}
}
