/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package resources;

import java.util.*;

public class ResourceExtent {
	private static Map<String, Resource> resources = new Hashtable<String, Resource>();
	private static int idCounter = 0;
	public static ResourceExtent INSTANCE = new ResourceExtent();
	
	private ResourceExtent() {
		// Initialise and register library resources
		createResource(new Resource("Book", "Lord of the Rings", "J.R.Tolkien"));
		createResource(new Resource("Book", "2001 Space Odessey", "Arthur C Clarke"));
		createResource(new Resource("Book", "How to win at bingo", "Two fat ladies"));
		createResource(new Resource("Book", "The Cold. Don't let it kill you.", "J.R.Hartley"));
		createResource(new Resource("Game", "Halo", "X-Box"));
		createResource(new Resource("Game", "Midtown madness", "PS2"));
	}
	public synchronized void createResource(Resource resource) {
		if (resource == null) {
			throw new NullPointerException("Resource can not be null");
		}
		resource.setId(getNextId());
		resources.put(resource.getId(), resource);
	}
	public synchronized Resource findByPrimaryKey(String id) {
		return resources.get(id);
	}
	public synchronized Collection<Resource> getResources() {
		return new ArrayList<Resource>(resources.values());
	}
	public synchronized String getNextId() {
		return "R" + (idCounter++);
	}
}
