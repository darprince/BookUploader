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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataBase {
    static Logger log = LogManager.getLogger(DataBase.class.getName());

    private static Connection connect = null;
    private static Statement statement = null;

    /**
     * Takes a list of ISBNs, finds one that goodreads recognizes, populates the
     * {@link Book} and inserts into table.
     *
     * @param isbnList
     *            List of isbns returned from google API related to the book
     *            being queried
     * @param book
     *            {@link Book}
     * @return True if table insert succeeded, false otherwise.
     */
    public static boolean insertFromUpload(List<String> isbnList, Book book) {
        connectToDB();

        log.info("ISBN list size: " + isbnList.size());
        try {
            return getBookInfoAndInsert(isbnList, book);
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Queries the database for needsProccessing = 1, populates book, and
     * updates database table.
     *
     * @return True if Successful, false otherwise.
     */
    public static boolean getFromDBandUpdate() {
        connectToDB();

        final List<String> isbnList = getIsbnsFromDBToQuery();

        printISBNs(isbnList);

        return getBookInfoAndUpdate(isbnList);
    }

    /**
     * @param isbnList
     *            List of isbns returned from google API.
     * @param book
     *            {@link Book}
     */
    public static boolean getBookInfoAndInsert(List<String> isbnList, Book book) {
        for (final String bookIsbn : isbnList) {
            try {
                final String completeBookInfo = Goodreads.getCompleteBookInfo(bookIsbn);
                final Book finalBook = populateBook(bookIsbn, completeBookInfo, book);
                final boolean inserted = finalBook.insertToTable(connect);
                if (inserted) {
                    log.info("Book is fully populated: " + finalBook.toString() + "\n\n");
                    log.info("Inserted ISBN: " + finalBook.isbn);
                    return true;
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * @param isbnList
     *            List of isbns returned from google API.
     * @return True if successful, false otherwise.
     */
    public static boolean getBookInfoAndUpdate(List<String> isbnList) {
        for (final String bookIsbn : isbnList) {
            try {
                final Book book = Book.getInstance();
                final String completeBookInfo = Goodreads.getCompleteBookInfo(bookIsbn);
                final Book finalBook = populateBook(bookIsbn, completeBookInfo, book);
                finalBook.updateTable(connect);
            } catch (final Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Takes a {@link JsonObject} in {@link String} form and populates
     * {@link Book} object.
     *
     * @param isbn
     * @param completeBookInfo
     * @param book
     *            Partially populated {@link Book}.
     * @return Fully populated {@link Book}.
     */
    private static Book populateBook(String isbn, String completeBookInfo, Book book) {
        final JsonParser parser = new JsonParser();
        JsonObject jsonBookInfo;
        jsonBookInfo = (JsonObject) parser.parse(completeBookInfo);
        final JsonObject jsonBook = jsonBookInfo.get("book").getAsJsonObject();
        final JsonObject jsonSeries = jsonBookInfo.get("series").getAsJsonObject();
        final Integer year = jsonBook.get("year").getAsInt();
        final Double rating = jsonBook.get("rating").getAsDouble();
        final String goodreadsId = jsonBook.get("goodreadsId").getAsString();
        final String title = jsonBook.get("title").getAsString();
        final String image = jsonBook.get("image").getAsString();
        final Integer authorId = jsonBook.get("authorID").getAsInt();
        final String author = jsonBook.get("authorName").getAsString();
        final Integer pages = jsonBook.get("pages").getAsInt();
        final String synopsis = jsonBook.get("synopsis").getAsString();
        final String genre = jsonBook.get("genres").getAsString();
        final String googleId = jsonBook.get("googleId").getAsString();

        final Integer seriesNumber = jsonSeries.get("seriesNumber").getAsInt();
        final String seriesIdString = jsonSeries.get("seriesID").getAsString();

        Integer seriesId = null;
        if (seriesIdString.equals("") || seriesIdString == null) {
            seriesId = 0;
        }

        if (book.author == null || book.author.equals("")) {
            book.setAuthor(author);
            book.setTitle(title);
        }
        book.setIsbn(isbn);
        book.setAuthorId(authorId);
        book.setRating(rating);
        book.setSynopsis(synopsis);
        book.setPages(pages);
        book.setSeriesId(seriesId);
        book.setSeriesNumber(seriesNumber);
        book.setYear(year);
        book.setgenre(genre);
        book.setImage(image);
        book.setGoogleId(googleId);
        book.setGoodreadsId(goodreadsId);

        return book;
    }

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
            final ResultSet resultSet = statement.executeQuery(
                    "select count(*) as count from princewalker.books where isbn = " + isbn);
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
            connect = DriverManager
                    .getConnection("jdbc:mysql://princewalker.db.9903859.hostedresource.com?"
                            + "user=princewalker&password=W@lker75");

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
