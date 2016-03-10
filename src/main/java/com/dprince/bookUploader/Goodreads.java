package com.dprince.bookUploader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
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
    public static boolean getGoodreadsBookInfo(String isbn) {
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(QUERY, isbn));
        parameters.add(new BasicNameValuePair(KEY, MY_KEY));
        final URL url = Helpers.buildURL(SEARCH_CONTEXT, parameters, HOST);

        final String xml = Helpers.getStringResponseFromURL(url);

        return parseXMLFromGoodreads(xml, isbn);
    }

    /**
     * @param xml
     *            Response from goodreads API.
     * @param isbn
     */
    public static boolean parseXMLFromGoodreads(String xml, String isbn) {

        System.out.println("Trying isbn: " + isbn);
        DocumentBuilder builder = null;

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

            final String workId = getNode(workNodes, "id").getTextContent();
            final String year = getNode(workNodes, "original_publication_year").getTextContent();
            final String rating = getNode(workNodes, "average_rating").getTextContent();

            final Node bestNode = getNode(workNodes, "best_book");
            final NodeList bestNodes = bestNode.getChildNodes();

            final String goodreadsId = getNode(bestNodes, "id").getTextContent();
            final String title = getNode(bestNodes, "title").getTextContent();

            final Node authorNode = getNode(bestNodes, "author");
            final NodeList authorNodes = authorNode.getChildNodes();

            final String authorID = getNode(authorNodes, "id").getTextContent();
            final String authorName = getNode(authorNodes, "name").getTextContent();

            final Book book = Book.getInstance();

            if (!StringUtils.containsIgnoreCase(title, book.title)
                    || !book.author.equals(authorName) || isbn == null || isbn.equals("")) {
                System.out.println(
                        "Rejecting Book: " + book.author + " - " + book.title + " - " + isbn);
                System.out.println(authorName + " - " + title + "\n\n");
                return false;
            }

            book.setIsbn(isbn);
            book.setWorkId(workId);
            book.setYear(Integer.parseInt(year));
            book.setRating(Double.parseDouble(rating));
            book.setGoodreadsId(goodreadsId);
            book.setTitle(title);
            book.setAuthorId(Integer.parseInt(authorID));
            book.setAuthor(authorName);

            addSeriesInfoFromGoodreads(book, workId);

            System.out.println("************************************************");
            System.out.println("ISBN: " + book.isbn);
            System.out.println("Author: " + book.author + " " + book.authorId);
            System.out.println("Title: " + book.title + "\n\n");

            final Scanner in = new Scanner(System.in);
            System.out.println("Correct?");
            final String userResponse = in.nextLine();
            in.close();

            if (userResponse.equalsIgnoreCase("y")) {
                return true;
            }

            return false;
        } catch (ParserConfigurationException | SAXException | NullPointerException
                | IOException e1) {
            System.out.println("Error parsing Goodreads info.");
        }
        return false;
    }

    /**
     * @param workID
     *            Goodreads general ID code for the book.
     */
    private static void addSeriesInfoFromGoodreads(Book book, String workId) {
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(FORMAT, "xml"));
        parameters.add(new BasicNameValuePair(KEY, MY_KEY));
        final String seriesContext = SERIES_CONTEXT1 + workId + SERIES_CONTEXT2;

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

            final String seriesId = getNode(seriesNodes, "id").getTextContent();

            book.setSeriesNumber(Integer.parseInt(seriesNumber));
            book.setSeriesId(Integer.parseInt(seriesId));

        } catch (ParserConfigurationException | SAXException | NullPointerException
                | IOException e1) {

            book.setSeriesNumber(0);
            book.setSeriesId(0);
        }
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
