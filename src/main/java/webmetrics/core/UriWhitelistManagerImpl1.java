/**
 * This class helps build white-listed collection of URIs and URI Patterns.<br>
 */
package webmetrics.core;

public class UriWhitelistManagerImpl1 extends AbstractWhitelistURIManager implements WhitelistURIManager {

	@Override
	protected UriPatternManager getUriPatternManager() {
		return new UriPatternManagerImpl1();
	}

}
