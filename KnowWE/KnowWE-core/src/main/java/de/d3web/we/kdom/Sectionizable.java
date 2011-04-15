package de.d3web.we.kdom;

import de.d3web.we.kdom.sectionFinder.SectionFinder;


public interface Sectionizable {

	/**
	 * @param sectionFinder is the SectionFinder that is later returned by
	 *        {@link Sectionizable#getSectionFinder()};
	 */
	public void setSectionFinder(SectionFinder sectionFinder);

	/**
	 * @return if a SectionFinder was set using
	 *         {@link Sectionizable#setSectionFinder(SectionFinder)}, that
	 *         SectionFinder should be returned. Else return a default
	 *         SectionFinder.
	 */
	public abstract SectionFinder getSectionFinder();

}