package com.dprince.bookUploader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Holds all the methods related to the Google Books Api.
 *
 * @author Darren
 *
 */
public class Google {
	static Logger log = LogManager.getLogger(Google.class.getName());

	private static final String GOOGLE_API_KEY = "AIzaSyDJ_9p14z3iZhKcUcN7xrNrt1LTBWk7mX4";

	/**
	 * @param author
	 * @param title
	 * @return A {@link List<String>} of all isbns related to the book being
	 *         queried.
	 */
	public static List<String> getPotentialIsbnsFromGoogle(String author, String title) {
		String jsonString = null;
		final List<String> isbnList = new ArrayList<String>();

		final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("q", title + "+inauthor:" + author));
		parameters.add(new BasicNameValuePair("key", GOOGLE_API_KEY));

		final URL url = Helpers.buildURL("books/v1/volumes", parameters, "www.googleapis.com/");
		log.info(url);

		jsonString = Helpers.getStringResponseFromURL(url);

		final JsonParser parser = new JsonParser();
		final JsonObject jsonBase = (JsonObject) parser.parse(jsonString);
		final JsonArray jsonItems = jsonBase.get("items").getAsJsonArray();

		for (final JsonElement element : jsonItems) {
			try {
				final JsonObject elementObject = element.getAsJsonObject();
				final JsonObject jsonVolumeInfo = elementObject.get("volumeInfo").getAsJsonObject();

				final String isbn1 = jsonVolumeInfo.get("industryIdentifiers").getAsJsonArray().get(0).getAsJsonObject()
						.get("identifier").getAsString();
				isbnList.add(isbn1);
				final String isbn2 = jsonVolumeInfo.get("industryIdentifiers").getAsJsonArray().get(1).getAsJsonObject()
						.get("identifier").getAsString();
				isbnList.add(isbn2);
			} catch (final Exception e) {

			}
		}

		log.info("returning from getIsbnFromGoogle");
		return isbnList;
	}

	/**
	 * @param json
	 *            The jsonObject partially populated from goodreads.
	 * @param isbn
	 * @return A {@link JsonObject} fully populated with complete book info.
	 */
	public static JsonObject addGoogleInfo(JsonObject json, String isbn) {
		URL url = null;
		try {
			url = new URL("https://www.googleapis.com/books/v1/volumes?q=" + isbn);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}

		final String googleJson = Helpers.getStringResponseFromURL(url);

		final JsonParser parser = new JsonParser();
		final JsonObject jsonBase = (JsonObject) parser.parse(googleJson);

		final JsonObject items = (JsonObject) jsonBase.get("items").getAsJsonArray().get(0);
		final String googleId = items.get("id").getAsString();

		final JsonObject volumeInfo = items.get("volumeInfo").getAsJsonObject();
		final int pages = volumeInfo.get("pageCount").getAsInt();
		final String synopsis = volumeInfo.get("description").getAsString();
		final String genres = volumeInfo.get("categories").getAsString();
		final String image = volumeInfo.get("imageLinks").getAsJsonObject().get("thumbnail").getAsString();

		final JsonObject jsonBook = json.get("book").getAsJsonObject();
		jsonBook.addProperty("pages", pages);
		jsonBook.addProperty("synopsis", synopsis);
		jsonBook.addProperty("genres", genres);
		jsonBook.addProperty("googleId", googleId);
		jsonBook.addProperty("image", image);

		final JsonObject toReturn = new JsonObject();
		toReturn.add("book", jsonBook);
		try {
			final JsonObject jsonSeries = json.get("series").getAsJsonObject();
			toReturn.add("series", jsonSeries);
		} catch (final NullPointerException e) {

		}

		return toReturn;
	}
}
