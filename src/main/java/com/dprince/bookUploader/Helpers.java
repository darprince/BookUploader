package com.dprince.bookUploader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

public class Helpers {
	private static final String SCHEME = "https";

	/**
	 * @param context
	 * @param parameters
	 * @param host
	 * @return Built URL. {@link URL}
	 */
	public static URL buildURL(String context, List<NameValuePair> parameters, String host) {
		if (parameters == null) {
			parameters = new ArrayList<NameValuePair>();
		}

		final URIBuilder builder = new URIBuilder().setScheme(SCHEME).setPath(context).setHost(host)
				.setParameters(parameters);
		try {
			return new URL(builder.toString());
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param url
	 *            URL to be queried
	 * @return String representation of URL's Json or XML
	 */
	public static String getStringResponseFromURL(URL url) {
		HttpURLConnection conn;
		String line = null;
		StringBuilder sb = null;

		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.connect();

			final InputStream inputStream = conn.getInputStream();
			BufferedReader br = null;
			sb = new StringBuilder();

			br = new BufferedReader(new InputStreamReader(inputStream));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}
}
