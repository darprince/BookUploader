package com.dprince.bookUploader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

/**
 * Holds all the methods related to the Goodreads API.
 *
 * @author Darren
 *
 */
public class Goodreads {
	static Logger log = LogManager.getLogger(Goodreads.class.getName());

	private static final String UTF_8 = "UTF-8";
	private static final String MY_KEY = "r8x5z4aKtjf38LBjAGcBmA";
	private static final String HOST = "www.goodreads.com/";
	private static final String SERIES_CONTEXT1 = "work/";
	private static final String SERIES_CONTEXT2 = "/series";
	private static final String KEY = "key";
	private static final String FORMAT = "format";
	private static final String QUERY = "q";
	private static final String SEARCH_CONTEXT = "search/index.xml";

	/**
	 * @param isbn
	 * @return A fully populated {@link JsonObject} in {@link String} form.
	 */
	public static String getCompleteBookInfo(String isbn) {
		final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair(QUERY, isbn));
		parameters.add(new BasicNameValuePair(KEY, MY_KEY));
		final URL url = Helpers.buildURL(SEARCH_CONTEXT, parameters, HOST);
		final String xml = Helpers.getStringResponseFromURL(url);
		return parseXMLFromGoodreads(xml, isbn).toString();
	}

	/**
	 * @param xml
	 *            Response from goodreads API.
	 * @param isbn
	 * @return A fully populated {@link JsonObject} including response from
	 *         Google API.
	 */
	public static JsonObject parseXMLFromGoodreads(String xml, String isbn) {
		DocumentBuilder builder = null;
		final JsonObject json = new JsonObject();
		JsonObject toReturn = null;

		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();

			final ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(UTF_8));
			final Document doc = builder.parse(input);

			final Element root = doc.getDocumentElement();
			final NodeList nodeList = root.getElementsByTagName("search");

			final Node search = getNode(nodeList, "search");
			final NodeList searchNodes = search.getChildNodes();

			final Node resultsNode = getNode(searchNodes, "results");
			final NodeList resultsNodes = resultsNode.getChildNodes();

			final Node workNode = getNode(resultsNodes, "work");
			final NodeList workNodes = workNode.getChildNodes();

			final String workID = getNode(workNodes, "id").getTextContent();
			final String year = getNode(workNodes, "original_publication_year").getTextContent();
			final String rating = getNode(workNodes, "average_rating").getTextContent();

			final Node bestNode = getNode(workNodes, "best_book");
			final NodeList bestNodes = bestNode.getChildNodes();

			final String goodreadsID = getNode(bestNodes, "id").getTextContent();
			final String title = getNode(bestNodes, "title").getTextContent();

			final Node authorNode = getNode(bestNodes, "author");
			final NodeList authorNodes = authorNode.getChildNodes();

			final String authorID = getNode(authorNodes, "id").getTextContent();
			final String authorName = getNode(authorNodes, "name").getTextContent();

			final JsonObject jsonBook = new JsonObject();

			jsonBook.addProperty("workID", workID);
			jsonBook.addProperty("year", year);
			jsonBook.addProperty("rating", rating);
			jsonBook.addProperty("goodreadsId", goodreadsID);
			jsonBook.addProperty("title", title);
			jsonBook.addProperty("authorID", authorID);
			jsonBook.addProperty("authorName", authorName);
			jsonBook.addProperty("isbn", isbn);

			json.add("book", jsonBook);

			toReturn = addSeriesInfoFromGoodreads(json, workID);
			toReturn = Google.addGoogleInfo(json, isbn);

		} catch (ParserConfigurationException | SAXException | IOException e1) {
			e1.printStackTrace();
		}

		return toReturn;
	}

	/**
	 * @param json
	 *            A {@link JsonObject} populated without series and google info.
	 * @param workID
	 *            Goodreads general ID code for the book.
	 * @return A {@link JsonObject} populated with the series info.
	 */
	private static JsonObject addSeriesInfoFromGoodreads(JsonObject json, String workID) {
		final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair(FORMAT, "xml"));
		parameters.add(new BasicNameValuePair(KEY, MY_KEY));
		final String seriesContext = SERIES_CONTEXT1 + workID + SERIES_CONTEXT2;

		final URL url = Helpers.buildURL(seriesContext, parameters, HOST);
		final String seriesInfo = Helpers.getStringResponseFromURL(url);

		DocumentBuilder builder = null;
		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();

			final ByteArrayInputStream input = new ByteArrayInputStream(seriesInfo.getBytes(UTF_8));
			final Document doc = builder.parse(input);

			final Element root = doc.getDocumentElement();
			final NodeList rootList = root.getElementsByTagName("series_works");

			final Node seriesWorksNode = rootList.item(0);
			final NodeList seriesWorksList = seriesWorksNode.getChildNodes();

			final Node seriesWorkNode = getNode(seriesWorksList, "series_work");
			final NodeList seriesWorkNodes = seriesWorkNode.getChildNodes();
			final String seriesNumber = getNode(seriesWorkNodes, "user_position").getTextContent();

			final Node seriesNode = getNode(seriesWorkNodes, "series");
			final NodeList seriesNodes = seriesNode.getChildNodes();

			final String seriesID = getNode(seriesNodes, "id").getTextContent();
			final String seriesTitle = getNode(seriesNodes, "title").getTextContent();
			final String seriesDescription = getNode(seriesNodes, "description").getTextContent();
			final String seriesCount = getNode(seriesNodes, "series_works_count").getTextContent();

			final JsonObject jsonSeries = new JsonObject();

			jsonSeries.addProperty("seriesNumber", seriesNumber);
			jsonSeries.addProperty("seriesID", seriesID);
			jsonSeries.addProperty("seriesTitle", seriesTitle);
			jsonSeries.addProperty("seriesDescription", seriesDescription);
			jsonSeries.addProperty("seriesCount", seriesCount);

			json.add("series", jsonSeries);

		} catch (ParserConfigurationException | SAXException | NullPointerException | IOException e1) {
			final JsonObject jsonSeries = new JsonObject();

			jsonSeries.addProperty("seriesNumber", "0");
			jsonSeries.addProperty("seriesID", "");
			jsonSeries.addProperty("seriesTitle", "");
			jsonSeries.addProperty("seriesDescription", "");
			jsonSeries.addProperty("seriesCount", "");

			json.add("series", jsonSeries);
		}

		return json;
	}

	/**
	 * Used as a helper to sort through XML nodes.
	 *
	 * @param nodeList
	 *            {@link NodeList}
	 * @param nodeName
	 *            The name of the node to return from the nodeList.
	 * @return A XML node {@link Node}.
	 */
	public static Node getNode(NodeList nodeList, String nodeName) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			try {
				if (nodeList.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
					return nodeList.item(i);
				}
			} catch (final Exception e) {
				return null;
			}
		}
		return null;
	}
}
