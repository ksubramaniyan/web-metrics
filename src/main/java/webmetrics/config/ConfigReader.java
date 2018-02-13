/**
 * Reader for one configuration file
 * 
 */
package webmetrics.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import webmetrics.util.StringUtils;

public class ConfigReader {

	// XML config file schema as resource
	private String xmlSchema;
	// XML config file as resource
	private String xmlConfiguration;
	// ordered map: key - uri or uri pattern; value - metric name to display
	// this map allows multiple URIs/Patterns to share same Display for Metric
	private Map<String, String> whitelistedUris;
	// for each whitelisted URI there can exist whitelisted matching histogram
	// un-ordered map: key - display name; value - histogram
	private Map<String, Integer[]> whitelistedResponseBuckets;
	private String nonWhiteListName;
	private Integer[] responseBuckets;
	private String domain;
	private Boolean skipContextName;
	private Integer uriPatternVersion;
	private Boolean enableNonWhiteListedUri;
	private Boolean enableHistogram;
	private Boolean enableStatus;
	private String idField;
	private String idFieldScope;
	private Boolean enabled;
	private Map<String, String> consumers;
	private Set<String> disabledConsumers;

	private List<String> parseWarnings;
	private List<String> parseErrors;

	private StringUtils textUtil = StringUtils.getInstance();

	private final static String NODE_WHITELIST_URIS = "whiteListedUris";
	private final static String NODE_WHITELIST_URI = "whiteListedUri";
	private final static String NODE_URI = "uri";
	private final static String NODE_URI_DISPLAY = "display";
	private final static String NODE_HISTOGRAM = "histogram";
	private final static String NODE_DOMAIN = "domain";
	private final static String NODE_METRIC_PARAMS = "metrics-params";
	private final static String NODE_METRIC_PARAM = "metrics-param";
	private final static String NODE_PARAM_SKIP_CONTEXT = "skipContextName";
	private final static String NODE_PARAM_PATTRN_VER = "uriPatternVersion";
	private final static String NODE_PARAM_ENABLE_NON_WHITE = "enableNonWhiteListedUri";
	private final static String NODE_PARAM_ENABLE_HISTOGRAM = "enableHistogram";
	private final static String NODE_PARAM_ENABLE_STATUS = "enableStatus";
	private final static String NODE_PARAM_NON_WHITE_NAME = "nonWhiteListName";
	private final static String NODE_CONSUMERS = "consumers";
	private final static String NODE_ID_FIELD = "idField";
	private final static String NODE_ID_FIELD_SCOPE = "idFieldScope";
	private final static String NODE_CONSUMER = "consumer";

	private final static String ATTR_ID = "id";
	private final static String ATTR_ALIAS = "alias";
	private final static String ATTR_ENABLED = "enabled";

	/**
	 * Constructor takes schema and content of one configuration file as resource names.<br>
	 * Content can be null in which case this configuration will not be used.<br>
	 * 
	 * @param xmlSchema
	 * @param xmlConfiguration
	 */
	public ConfigReader(String xmlSchema, String xmlConfiguration) {
		this.xmlSchema = xmlSchema;
		this.xmlConfiguration = xmlConfiguration;
		readConfiguration();
	}

	private void readConfiguration() {
		Document doc = getDocument();
		if (doc == null)
			return;
		buildEnabled(doc);
		buildWhitelistUris(doc);
		buildGlobalHistogram(doc);
		buildConsumers(doc);
		buildDomain(doc);
		buildMetricParams(doc);
	}

	/**
	 * Returns document after parsing given resource.<br>
	 * 
	 * @return
	 */
	private Document getDocument() {
		if (xmlSchema == null || xmlSchema.trim().equals(""))
			throw new IllegalArgumentException("Schema is required for web metrics configuration");
		if (xmlConfiguration == null || xmlConfiguration.trim().equals(""))
			return null;
		logger.info("Using Web Metrics configuration " + xmlConfiguration);

		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;
		try {
			URL urlResource = getClass().getClassLoader().getResource(xmlSchema);
			schema = schemaFactory.newSchema(urlResource);
		} catch (SAXException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Cannot read web metrics schema");
		}

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setSchema(schema);
		factory.setNamespaceAware(true);
		InputStream is = null;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new ErrorHandler() {

				@Override
				public void warning(SAXParseException exception) throws SAXException {
					if (parseWarnings == null)
						parseWarnings = new ArrayList<String>();
					parseWarnings.add("SAXParseException: " + exception.getMessage());
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					if (parseErrors == null)
						parseErrors = new ArrayList<String>();
					parseErrors.add("SAXParseException (fatal): " + exception.getMessage());
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					if (parseErrors == null)
						parseErrors = new ArrayList<String>();
					parseErrors.add("SAXParseException: " + exception.getMessage());
				}
			});
			is = textUtil.getInputStream(xmlConfiguration);
			Document doc = builder.parse(is);
			if (parseWarnings != null) {
				logger.warn("Found warnings while parsing Web Metrics Configuration " + xmlConfiguration + " : " + parseWarnings);
			}
			if (parseErrors != null) {
				logger.warn("Found errors while parsing Web Metrics Configuration " + xmlConfiguration + " : " + parseErrors);
				throw new IllegalArgumentException("Parsing error occurred, please see log for details");
			}
			return doc;
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException("ParserConfigurationException", e);
		} catch (SAXException e) {
			throw new IllegalArgumentException("SAXException", e);
		} catch (IOException e) {
			throw new IllegalArgumentException("IOException", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * Validates that value contains only single-line (no \r, \n).<br>
	 * 
	 * @param value
	 */
	private void validateSingleLine(String value) {
		if (value.contains("\r") || value.contains("\n"))
			throw new IllegalArgumentException("Web Metrics configuration value must be on single line, but found this: " + value);
	}

	private static final String REGEX_WHITE_CHARS = "\\s*+(.*?)\\s*+";
	private static final Pattern PATTERN_WHITE_CHARS = Pattern.compile(REGEX_WHITE_CHARS);

	/**
	 * Trims any leading and trailing white characters.<br>
	 * 
	 * @param value
	 * @return
	 */
	private String trimWhiteChar(String value) {
		Matcher m = PATTERN_WHITE_CHARS.matcher(value);
		if (m.matches())
			return m.group(1);
		return value;
	}

	private void buildEnabled(Document doc) {
		Node root = doc.getFirstChild();
		if (root.getAttributes() != null && root.getAttributes().getNamedItem(ATTR_ENABLED) != null) {
			if ("true".equals(root.getAttributes().getNamedItem(ATTR_ENABLED).getNodeValue())) {
				enabled = Boolean.TRUE;
			} else {
				enabled = Boolean.FALSE;
			}
		}
	}

	private void buildWhitelistUris(Document doc) {
		whitelistedUris = null;
		// there is actually just one whiteListedUris node
		NodeList whiteListUrisNodes = doc.getElementsByTagName(NODE_WHITELIST_URIS);
		if (whiteListUrisNodes == null || whiteListUrisNodes.getLength() == 0)
			return;
		Node whiteListUrisNode = whiteListUrisNodes.item(0);
		// there can be many whiteListedUri nodes
		NodeList whiteListUriNodes = whiteListUrisNode.getChildNodes();
		if (whiteListUriNodes == null || whiteListUriNodes.getLength() == 0)
			return;
		for (int i = 0; i < whiteListUriNodes.getLength(); i++) {
			// each whiteListedUri node will have one or many "uri", single "display" and optional single "histogram" elements
			Node whiteListUriNode = whiteListUriNodes.item(i);
			if (!NODE_WHITELIST_URI.equals(whiteListUriNode.getNodeName())) {
				continue;
			}
			NodeList uriList = whiteListUriNode.getChildNodes();
			if (uriList == null || uriList.getLength() == 0) {
				continue;
			}
			addUrisForDisplay(uriList);
		}
		logger.info("Built following whitelisted uris: " + whitelistedUris);
	}

	/**
	 * Assuming the uriList contains all nodes specific to one "display" node (multiple uri and single histogram) will build a configuration for that display.<br>
	 * 
	 * @param uriList
	 *            - assumed non-empty
	 */
	private void addUrisForDisplay(NodeList uriList) {
		List<String> uris = null; // builds list of URIs/Patterns for single Display Metric
		String displayName = null;
		Integer[] histogram = null;
		for (int i = 0; i < uriList.getLength(); i++) {
			Node node = uriList.item(i);
			String text = trimWhiteChar(node.getTextContent());
			if (text.equals("")) {
				continue;
			}
			validateSingleLine(text);
			if (node.getNodeName().equals(NODE_URI)) {
				if (whitelistedUris != null && whitelistedUris.keySet().contains(text))
					throw new IllegalArgumentException("Uri is not unique: " + text);
				if (uris == null) {
					uris = new ArrayList<String>();
				}
				uris.add(text);
			} else if (node.getNodeName().equals(NODE_URI_DISPLAY)) {
				if (checkDisplayNameExists(text))
					throw new IllegalArgumentException("Display name is not unique: " + text);
				displayName = convertDisplayName(text);
			} else if (node.getNodeName().equals(NODE_HISTOGRAM)) {
				// build histogram
				histogram = buildNodeHistogram(node);
			}
		}
		if (displayName == null) {
			logger.error("Display element is not found");
			return;
		}
		if (uris == null) {
			logger.error("No uri elements found for display: " + displayName);
			return;
		}
		if (whitelistedUris == null) {
			whitelistedUris = new LinkedHashMap<String, String>(); // provides uniqueness and order
		}
		for (String uri : uris) {
			whitelistedUris.put(uri, displayName);
		}
		// assign optional histogram
		if (histogram != null) {
			logger.info("Assigning histogram for display name: " + displayName + " : " + textUtil.displayIntegerArray(histogram));
			if (whitelistedResponseBuckets == null) {
				whitelistedResponseBuckets = new HashMap<String, Integer[]>();
			}
			whitelistedResponseBuckets.put(displayName, histogram);
		}
	}

	/**
	 * Iterates over whitelistedUris and returns true if at least one entry found with given value.<br>
	 * 
	 * @param displayName
	 * @return
	 */
	private boolean checkDisplayNameExists(String displayName) {
		if (whitelistedUris == null)
			return false;
		for (String uri : whitelistedUris.keySet()) {
			String dispName = whitelistedUris.get(uri);
			if (dispName.equalsIgnoreCase(displayName))
				return true;
		}
		return false;
	}

	private void buildGlobalHistogram(Document doc) {
		logger.info("Building global histogram");
		responseBuckets = null;
		NodeList nodes = doc.getElementsByTagName(NODE_HISTOGRAM);
		if (nodes == null || nodes.getLength() == 0) {
			logger.info("No global histogram defined");
			return;
		}
		for (int i = 0; i < nodes.getLength(); i++) {
			Node histogramNode = nodes.item(i);
			if ("webmetric".equals(histogramNode.getParentNode().getNodeName())) {
				logger.info("Found right global histogram node");
				responseBuckets = buildNodeHistogram(histogramNode);
				break;
			}
		}
		if (responseBuckets == null) {
			logger.warn("Global Histogram buckets are not configured");
		}
	}

	private Integer[] buildNodeHistogram(Node histogramNode) {
		if (histogramNode == null)
			return null;
		String bucketsValue = histogramNode.getTextContent();
		if (bucketsValue == null)
			return null;
		bucketsValue = trimWhiteChar(bucketsValue);
		if (bucketsValue.equals(""))
			return null;
		validateSingleLine(bucketsValue);
		String[] bucketsValues = bucketsValue.trim().split(",");
		if (bucketsValues == null || bucketsValues.length == 0)
			return null;

		List<Integer> vs = new ArrayList<Integer>();
		for (String value : bucketsValues) {
			if (value == null)
				continue;
			value = value.trim();
			if (value.equals(""))
				continue;
			try {
				Integer intValue = Integer.valueOf(value);
				vs.add(intValue);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid configuration for histogram: " + bucketsValue, e);
			}
		}
		logger.info("Response histogram built for node " + histogramNode.getParentNode() + ": " + vs);
		if (vs.size() == 0)
			return null;
		Integer[] responseBucketsInternal = new Integer[vs.size()];
		int idx = 0;
		for (Integer val : vs) {
			responseBucketsInternal[idx++] = val.intValue();
		}
		validateHistogramBuckets(responseBucketsInternal);
		return responseBucketsInternal;
	}

	private void validateHistogramBuckets(Integer[] responseBuckets) {
		// validate buckets
		if (responseBuckets != null && responseBuckets.length > 0) {
			// validate consecutiveness
			int lastBoundary = 0; // any boundary must be positive and consecutive, non-equal to previous
			for (int boundary : responseBuckets) {
				if (boundary <= lastBoundary) {
					String error = "Invalid histogram bucket configuration: boundaries must be consecutive and greater than 0";
					logger.error(error);
					throw new IllegalArgumentException(error);
				}
				lastBoundary = boundary;
			}
			// set to use buckets only if at least one bucket available and config setting is true
			// useBuckets = webMetricConfig.isEnableHistogram();
		} else {
			logger.warn("Histogram buckets are not configured");
		}
	}

	private void buildConsumers(Document doc) {
		NodeList nodes = doc.getElementsByTagName(NODE_CONSUMERS);
		if (nodes == null || nodes.getLength() == 0)
			return;
		Node consumersNode = null;
		for (int i = 0; i < nodes.getLength(); i++) {
			consumersNode = nodes.item(i);
			if ("webmetric".equals(consumersNode.getParentNode().getNodeName())) {
				logger.info("Found right consumers node");
				break;
			}
		}
		if (consumersNode == null) {
			return;
		}
		if (consumersNode.getAttributes() != null && consumersNode.getAttributes().getNamedItem(ATTR_ENABLED) != null) {
			if (!"true".equals(consumersNode.getAttributes().getNamedItem(ATTR_ENABLED).getNodeValue())) {
				// consumers node not enabled
				return;
			}
		}
		NodeList consumersNodesList = consumersNode.getChildNodes();
		if (consumersNodesList == null || consumersNodesList.getLength() == 0) {
			return;
		}
		for (int i = 0; i < consumersNodesList.getLength(); i++) {
			Node node = consumersNodesList.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			String text = node.getTextContent();
			if (NODE_ID_FIELD.equals(node.getNodeName())) {
				if (text == null || text.trim().equals(""))
					throw new IllegalArgumentException("ID FIELD must have value");
				idField = text.trim();
			} else if (NODE_ID_FIELD_SCOPE.equals(node.getNodeName())) {
				if (text == null || text.trim().equals(""))
					throw new IllegalArgumentException("ID FIELD SCOPE must have value");
				idFieldScope = text.trim();
			} else if (NODE_CONSUMER.equals(node.getNodeName())) {
				buildConsumerNode(node);
			}
		}
		if (idField == null || idFieldScope == null)
			throw new IllegalArgumentException("ID FIELD or ID FIELD SCOPE is not defined for consumers");
		logger.info("All consumers: " + consumers);
		logger.info("All disabled consumers: " + disabledConsumers);
	}

	private void buildConsumerNode(Node consumerNode) {
		if (consumerNode.getAttributes() == null)
			return;
		String id = null;
		String alias = null;
		Node idNode = consumerNode.getAttributes().getNamedItem(ATTR_ID);
		Node aliasNode = consumerNode.getAttributes().getNamedItem(ATTR_ALIAS);
		Node enabledNode = consumerNode.getAttributes().getNamedItem(ATTR_ENABLED);
		if (idNode == null)
			throw new IllegalArgumentException("Consumer node must have attribute id");
		id = idNode.getNodeValue();
		if (id == null || id.trim().equals(""))
			throw new IllegalArgumentException("Consumer id must have non-blank value");
		id = id.trim();

		if (consumers != null && consumers.containsKey(id))
			throw new IllegalArgumentException("Consumer id must be unique: " + id);

		if (disabledConsumers != null && disabledConsumers.contains(id))
			throw new IllegalArgumentException("Consumer id must be unique: " + id);

		if (enabledNode != null) {
			if (!"true".equals(enabledNode.getNodeValue())) {
				if (disabledConsumers == null)
					disabledConsumers = new HashSet<String>();
				disabledConsumers.add(id);
				return;
			}
		}

		if (aliasNode != null) {
			alias = aliasNode.getNodeValue();
			if (alias != null && !alias.trim().equals("")) {
				// make sure alias is one word
				alias = alias.trim();
				Matcher m = PATTERN_APLHANUMERIC_UNDERSCORE.matcher(alias);
				if (!m.matches())
					throw new IllegalArgumentException("Consumer alias can have only alpha-numeric and underscore characters. Consumer id: " + id + ", alias: "
									+ alias);
			} else {
				alias = null;
			}
		}
		if (alias == null) {
			// derive alias from id
			String idTemp = id;
			Matcher m = PATTERN_APLHANUMERIC_UNDERSCORE_FILTER.matcher(idTemp);
			StringBuilder sb = new StringBuilder();
			while (m.matches()) {
				sb.append(m.group(1));
				idTemp = m.group(2);
				m = PATTERN_APLHANUMERIC_UNDERSCORE_FILTER.matcher(idTemp);
			}
			if (sb.length() == 0)
				throw new IllegalArgumentException("Cannot build alias from id: " + id);
			alias = sb.toString();
		}
		if (consumers == null)
			consumers = new HashMap<String, String>();
		consumers.put(id, alias);
	}

	private static final String REGEX_APLHANUMERIC_UNDERSCORE = "[a-zA-Z0-9_]+";
	private static final Pattern PATTERN_APLHANUMERIC_UNDERSCORE = Pattern.compile(REGEX_APLHANUMERIC_UNDERSCORE);

	private static final String REGEX_APLHANUMERIC_UNDERSCORE_FILTER = "[^a-zA-Z0-9_]*([a-zA-Z0-9_]+)(.*)";
	private static final Pattern PATTERN_APLHANUMERIC_UNDERSCORE_FILTER = Pattern.compile(REGEX_APLHANUMERIC_UNDERSCORE_FILTER);

	private void buildDomain(Document doc) {
		domain = null;
		NodeList nodes = doc.getElementsByTagName(NODE_DOMAIN);
		if (nodes == null || nodes.getLength() == 0)
			return;
		Node node = nodes.item(0);
		domain = node.getTextContent();
		if (domain == null)
			return;
		domain = trimWhiteChar(domain);
		validateSingleLine(domain);
		logger.info("Domain: " + domain);
	}

	private void buildMetricParams(Document doc) {
		skipContextName = null;
		uriPatternVersion = 2;
		enableNonWhiteListedUri = null;
		enableHistogram = null;
		enableStatus = null;
		nonWhiteListName = null;
		NodeList nodes = doc.getElementsByTagName(NODE_METRIC_PARAMS);
		if (nodes == null || nodes.getLength() == 0)
			return;
		Node node = nodes.item(0);
		nodes = node.getChildNodes();
		if (nodes == null || nodes.getLength() == 0)
			return;
		for (int i = 0; i < nodes.getLength(); i++) {
			node = nodes.item(i);
			if (!NODE_METRIC_PARAM.equals(node.getNodeName()))
				continue;
			NodeList childParamNodes = node.getChildNodes();
			if (childParamNodes == null || childParamNodes.getLength() == 0)
				continue;
			String paramName = null;
			String paramValue = null;
			for (int j = 0; j < childParamNodes.getLength(); j++) {
				Node childParamNode = childParamNodes.item(j);
				String value = childParamNode.getTextContent();
				if (value == null)
					continue;
				value = trimWhiteChar(value);
				validateSingleLine(value);
				String name = childParamNode.getNodeName();
				validateSingleLine(name);
				if ("name".equals(name))
					paramName = value;
				if ("value".equals(name))
					paramValue = value;
			}
			if (paramName != null && paramValue != null)
				assignMetricParam(paramName, paramValue);
		}
	}

	private void assignMetricParam(String paramName, String paramValue) {
		if (paramName.equals(NODE_PARAM_SKIP_CONTEXT)) {
			try {
				skipContextName = Boolean.parseBoolean(paramValue);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid parameter value for " + NODE_PARAM_SKIP_CONTEXT);
			}
		} else if (paramName.equals(NODE_PARAM_PATTRN_VER)) {
			try {
				uriPatternVersion = Integer.parseInt(paramValue);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid parameter value for " + NODE_PARAM_PATTRN_VER);
			}
		} else if (paramName.equals(NODE_PARAM_ENABLE_NON_WHITE)) {
			try {
				enableNonWhiteListedUri = Boolean.parseBoolean(paramValue);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid parameter value for " + NODE_PARAM_ENABLE_NON_WHITE);
			}
		} else if (paramName.equals(NODE_PARAM_ENABLE_HISTOGRAM)) {
			try {
				enableHistogram = Boolean.parseBoolean(paramValue);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid parameter value for " + NODE_PARAM_ENABLE_HISTOGRAM);
			}
		} else if (paramName.equals(NODE_PARAM_ENABLE_STATUS)) {
			try {
				enableStatus = Boolean.parseBoolean(paramValue);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid parameter value for " + NODE_PARAM_ENABLE_STATUS);
			}
		} else if (paramName.equals(NODE_PARAM_NON_WHITE_NAME)) {
			nonWhiteListName = paramValue;
		}
	}

	/**
	 * Converts any forward slash to dot: "/" -> "."<br>
	 * Trims any trailing or leading dots.<br>
	 * 
	 * @param displayName
	 * @return
	 */
	private String convertDisplayName(String displayName) {
		if (displayName == null)
			return "";
		displayName = displayName.trim();
		if (displayName.equals(""))
			return "";
		while (displayName.startsWith("/"))
			displayName = displayName.substring(1);
		while (displayName.endsWith("/"))
			displayName = displayName.substring(0, displayName.length() - 1);
		displayName = displayName.replaceAll("/", ".");
		return displayName;
	}

	public Map<String, String> getWhitelistedUris() {
		return whitelistedUris;
	}

	public String getNonWhiteListName() {
		return nonWhiteListName;
	}

	public Integer[] getResponseBuckets() {
		return responseBuckets;
	}

	public Map<String, Integer[]> getWhitelistedResponseBuckets() {
		return whitelistedResponseBuckets;
	}

	public String getDomain() {
		return domain;
	}

	public Boolean isSkipContextName() {
		return skipContextName;
	}

	public Integer getUriPatternVersion() {
		return uriPatternVersion;
	}

	public Boolean isEnableNonWhiteListedUri() {
		return enableNonWhiteListedUri;
	}

	public Boolean isEnableHistogram() {
		return enableHistogram;
	}

	public Boolean isEnableStatus() {
		return enableStatus;
	}

	public String getIdField() {
		return idField;
	}

	public String getIdFieldscope() {
		return idFieldScope;
	}

	public Map<String, String> getConsumers() {
		return consumers;
	}

	public Set<String> getDisabledConsumers() {
		return disabledConsumers;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	private static Logger logger = LoggerFactory.getLogger(ConfigReader.class);
}