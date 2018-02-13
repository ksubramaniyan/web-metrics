/**
 * Various text/string based utils
 */
package webmetrics.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class StringUtils {

	private static final StringUtils textUtil = new StringUtils();

	public static StringUtils getInstance() {
		return textUtil;
	}

	private StringUtils() {
	}

	/**
	 * Assumes resource identifies some text based URL resource (file built into JAR or HTTP resource, etc) this method will attempt to obtain and return
	 * content of that resource.<br>
	 * Note: this method will use getContent(URL) internally.<br>
	 * 
	 * @param resource
	 */
	public String getContent(String resource) {
		URL urlResource = getClass().getClassLoader().getResource(resource);
		return getContent(urlResource);
	}

	public InputStream getInputStream(String resource) {
		URL urlResource = getClass().getClassLoader().getResource(resource);
		try {
			return urlResource.openStream();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot obtain input stream from resource " + resource, e);
		}
	}

	/**
	 * Assumes content of this resource is text will return content.<br>
	 * In case input stream cannot be obtained from this resource IllegalStateExceptionh is thrown wrapping actual exception.<br>
	 * Note: this method uses getContent(InputStream) internally.<br>
	 * 
	 * @param resource
	 * @return
	 */
	public String getContent(URL resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource is null");
		try {
			return getContent(resource.openStream());
		} catch (IOException e) {
			throw new IllegalStateException("Cannot obtain input stream from resource " + resource, e);
		}
	}

	/**
	 * Assumes input stream is text stream creates and returns String object while reading stream.<br>
	 * Stream will be closed at the ends of read.<br>
	 * Since text is assumed to possibly have new-line characters, this method will add "\n" at the end of every line of text in system-dependent manner.<br>
	 * In case stream cannot be read due to some errors, throws IllegalStateException wrapping actual exception produced by stream.<br>
	 * Note: this is IO blocking method.<br>
	 * 
	 * @param is
	 * @return
	 */
	public String getContent(InputStream is) {
		if (is == null)
			throw new IllegalArgumentException("Input Stream is null");
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			StringBuilder sb = new StringBuilder();
			while (line != null) {
				if (sb.length() > 0)
					sb.append("\n");
				sb.append(line);
				line = br.readLine();
			}
			br.close();
			return sb.toString();
		} catch (IOException e) {
			throw new IllegalStateException("Error reading input stream", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Strips trailing forward slash if exits and is not the only character.<br>
	 * 
	 * @param uri
	 * @return
	 */
	public String stripTrailingSlash(String uri) {
		if (uri.length() > 1 && uri.endsWith("/"))
			uri = uri.substring(0, uri.length() - 1);
		return uri;
	}

	/**
	 * Strips leading forward slash if exits and is not the only character.<br>
	 * 
	 * @param uri
	 * @return
	 */
	public String stripLeadingSlash(String uri) {
		if (uri.length() > 1 && uri.startsWith("/"))
			uri = uri.substring(1, uri.length());
		return uri;
	}

	/**
	 * Splits by "/", removes any empty element, removes trailing and leading "/", removes context name (first element) if required.<br>
	 * 
	 * @param uri
	 * @param stripContextName
	 * @return
	 */
	protected String getFormattedMetricName(String uri, boolean stripContextName) {
		boolean contextRemoved = false;
		String[] uriElements = uri.split("\\/");
		StringBuilder sb = new StringBuilder();
		for (String uriElement : uriElements) {
			if (uriElement == null)
				continue;
			uriElement = uriElement.trim();
			if (uriElement.equals(""))
				continue;
			if (stripContextName && !contextRemoved) {
				contextRemoved = true;
				continue;
			}
			if (sb.length() > 0)
				sb.append('.');
			sb.append(uriElement);
		}
		return sb.toString();
	}

	public String displayIntegerArray(Integer[] values) {
		StringBuilder sb = new StringBuilder();
		if(values == null) return "(null)";
		if(values.length == 0) return "(empty)";
		for(Integer value : values) {
			if(sb.length() == 0) sb.append('[');
			else sb.append(',');
			sb.append(value);
		}
		sb.append(']');
		return sb.toString();
	}

}
