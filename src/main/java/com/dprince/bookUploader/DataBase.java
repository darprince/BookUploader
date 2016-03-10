package com.dprince.bookUploader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBase {
	static Logger log = LogManager.getLogger(DataBase.class.getName());

	private static Connection connect = null;
	private static Statement statement = null;

	/**
	 * Takes a list of ISBNs, finds one that goodreads recognizes, populates the
	 * {@link Book} and inserts into table.
	 *
	 * @param book
	 *            {@link Book}
	 * @return True if table insert succeeded, false otherwise.
	 */
	public static boolean insertFromUpload(Book book) {
		connectToDB();

		if (book.insertToTable(connect)) {
			log.info("Inserted ISBN: " + book.isbn);
			return true;
		}
		return false;
	}

	/**
	 * Queries the database for needsProccessing = 1, populates book, and
	 * updates database table.
	 *
	 * @return True if Successful, false otherwise.
	 */
	// public static boolean getFromDBandUpdate() {
	// connectToDB();
	//
	// final List<String> isbnList = getIsbnsFromDBToQuery();
	//
	// printISBNs(isbnList);
	//
	// return getBookInfoAndUpdate(isbnList);
	// }

	/**
	 * @param isbnList
	 *            List of isbns returned from google API.
	 * @return True if successful, false otherwise.
	 */
	// public static boolean getBookInfoAndUpdate(List<String> isbnList) {
	// for (final String bookIsbn : isbnList) {
	// try {
	// final Book book = Book.getInstance();
	// Goodreads.getGoodreadsBookInfo(bookIsbn);
	// final Book finalBook = populateBook(bookIsbn, completeBookInfo, book);
	// finalBook.updateTable(connect);
	// } catch (final Exception e) {
	// e.printStackTrace();
	// return false;
	// }
	// }
	// return true;
	// }

	/**
	 * Queries the database for books with needsProcessing = 1.
	 *
	 * @return {@link List<String>} of isbns for books that need processing.
	 */
	public static List<String> getIsbnsFromDBToQuery() {
		final List<String> toReturn = new ArrayList<String>();

		try {
			final ResultSet resultSet = statement
					.executeQuery("select isbn from princewalker.books where needsProcessing = 1");
			while (resultSet.next()) {
				toReturn.add(resultSet.getString("isbn"));
			}
		} catch (final SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		if (toReturn.isEmpty()) {
			System.out.println("No books to query.");
			System.exit(0);
		}

		return toReturn;
	}

	public static boolean checkIfBookExistsInDB(String isbn) {
		connectToDB();

		try {
			final ResultSet resultSet = statement
					.executeQuery("select count(*) as count from princewalker.books where isbn = " + isbn);
			if (resultSet.getInt("count") == 1) {
				return true;
			}
		} catch (final SQLException e) {
			return false;
		}
		return false;
	}

	/**
	 * Connects to the database and initializes {@link Connection} and
	 * {@link Statement}.
	 */
	private static void connectToDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(
					"jdbc:mysql://princewalker.db.9903859.hostedresource.com?" + "user=princewalker&password=W@lker75");

			statement = connect.createStatement();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints isbns in isbnList.
	 *
	 * @param isbnList
	 */
	private static void printISBNs(List<String> isbnList) {
		for (final String bookIsbn : isbnList) {
			System.out.println(bookIsbn);
		}
	}
}
