package com.dprince.bookUploader;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Object to hold all details about the book being queried and uploaded.
 * {@link Book}
 *
 * @author Darren
 */
public class Book {
    static Logger log = LogManager.getLogger(Book.class.getName());

    String isbn;
    String filename;
    String fullFilename;
    String author;
    int authorId;
    String title;
    Double rating;
    String synopsis;
    int pages;
    Integer seriesId = null;
    int seriesNumber;
    int year;
    String genre;
    String image;
    String googleId;
    String goodreadsId;
    String workId;
    PreparedStatement preparedInsert;
    PreparedStatement preparedUpdate;

    private static Book instance = new Book();

    /**
     * A private Constructor prevents any other class from instantiating.
     */
    private Book() {
    }

    /**
     * @return Static 'instance' method. {@link Book}
     */
    public static Book getInstance() {
        return instance;
    }

    /**
     * @param isbnIn
     * @return {@link Book}
     */
    public Book setIsbn(String isbnIn) {
        this.isbn = isbnIn;
        return this;
    }

    /**
     * @param filenameIn
     * @return {@link Book}
     */
    public Book setFilename(String filenameIn) {
        this.filename = filenameIn;
        return this;
    }

    /**
     * @param fullFilenameIn
     *            Filename with complete path.
     * @return {@link Book}
     */
    public Book setFullFilename(String fullFilenameIn) {
        this.fullFilename = fullFilenameIn;
        return this;
    }

    /**
     * @param authorIn
     * @return {@link Book}
     */
    public Book setAuthor(String authorIn) {
        this.author = authorIn;
        return this;
    }

    /**
     * @param authorIdIn
     *            Goodreads authorId.
     * @return {@link Book}
     */
    public Book setAuthorId(int authorIdIn) {
        this.authorId = authorIdIn;
        return this;
    }

    /**
     * @param titleIn
     * @return {@link Book}
     */
    public Book setTitle(String titleIn) {
        this.title = titleIn;
        return this;
    }

    /**
     * @param ratingIn
     * @return {@link Book}
     */
    public Book setRating(Double ratingIn) {
        this.rating = ratingIn;
        return this;
    }

    /**
     * @param synopsisIn
     * @return {@link Book}
     */
    public Book setSynopsis(String synopsisIn) {
        this.synopsis = synopsisIn;
        return this;
    }

    /**
     * @param pagesIn
     * @return {@link Book}
     */
    public Book setPages(int pagesIn) {
        this.pages = pagesIn;
        return this;
    }

    /**
     * @param seriesIdIn
     *            Goodreads seriesId.
     * @return {@link Book}
     */
    public Book setSeriesId(Integer seriesIdIn) {
        this.seriesId = seriesIdIn;
        return this;
    }

    /**
     * @param seriesNumberIn
     *            The position of the book in the series.
     * @return {@link Book}
     */
    public Book setSeriesNumber(int seriesNumberIn) {
        this.seriesNumber = seriesNumberIn;
        return this;
    }

    /**
     * @param yearIn
     *            Year the book was published.
     * @return {@link Book}
     */
    public Book setYear(int yearIn) {
        this.year = yearIn;
        return this;
    }

    /**
     * @param genreIn
     * @return {@link Book}
     */
    public Book setgenre(String genreIn) {
        this.genre = genreIn;
        return this;
    }

    /**
     * @param imageIn
     * @return {@link Book}
     */
    public Book setImage(String imageIn) {
        this.image = imageIn;
        return this;
    }

    /**
     * @param googleIdIn
     *            The googleId of the book.
     * @return {@link Book}
     */
    public Book setGoogleId(String googleIdIn) {
        this.googleId = googleIdIn;
        return this;
    }

    /**
     * @param goodreadsIdIn
     *            The goodreadsId of the book.
     * @return {@link Book}
     */
    public Book setGoodreadsId(String goodreadsIdIn) {
        this.googleId = goodreadsIdIn;
        return this;
    }

    /**
     * @param workIdIn
     *            Id used to find series information from goodreads.
     * @return {@link Book}
     */
    public Book setWorkId(String workIdIn) {
        this.workId = workIdIn;
        return this;
    }

    /**
     * Takes the info from the {@link Book} object and inserts into the database
     * table.
     *
     * @param connect
     *            {@link Connection}
     * @return {@link Book}
     */
    public boolean insertToTable(Connection connect) {
        try {
            final String sql = "INSERT INTO princewalker.books (isbn, authorFromFile, titleFromFile, filename, author, authorID, otherAuthor, title, rating, synopsis, pages, seriesID, seriesNumber, year, genre, image, googleId, goodreadsId, dateAdded, similarBooks, needsProcessing)"
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,0);";

            preparedInsert = connect.prepareStatement(sql);
            preparedInsert.setString(1, isbn);
            preparedInsert.setString(2, null);
            preparedInsert.setString(3, null);
            preparedInsert.setString(4, filename);
            preparedInsert.setString(5, author);
            preparedInsert.setInt(6, authorId);
            preparedInsert.setString(7, null);
            preparedInsert.setString(8, title);
            preparedInsert.setDouble(9, rating);
            preparedInsert.setString(10, synopsis);
            preparedInsert.setInt(11, pages);
            preparedInsert.setInt(12, seriesId);
            preparedInsert.setInt(13, seriesNumber);
            preparedInsert.setInt(14, year);
            preparedInsert.setString(15, genre);
            preparedInsert.setString(16, image);
            preparedInsert.setString(17, googleId);
            preparedInsert.setString(18, goodreadsId);
            preparedInsert.setString(19, null);

            log.info(preparedInsert);

            final int executeUpdate = preparedInsert.executeUpdate();

            if (executeUpdate > 0) {
                System.out.println(this.toString() + "\n\n");
                return true;
            }
            return false;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Takes the info from the {@link Book} object and updates the database
     * table.
     *
     * @param connect
     *            {@link Connection}
     * @return {@link Book}
     */
    public boolean updateTable(Connection connect) {
        try {
            final String sql = "UPDATE princewalker.books set author =?, authorID =?, title =?, "
                    + "rating =?, synopsis =?, " + "pages =?, " + "seriesID =?, "
                    + "seriesNumber =?, " + "year =?, " + "genre =?, " + "image =?, "
                    + "googleId =?, " + "goodreadsId =?, "
                    + "dateAdded = NOW(), needsProcessing = 0 WHERE isbn =?;";

            preparedUpdate = connect.prepareStatement(sql);
            preparedUpdate.setString(1, author);
            preparedUpdate.setInt(2, authorId);
            preparedUpdate.setString(3, title);
            preparedUpdate.setDouble(4, rating);
            preparedUpdate.setString(5, synopsis);
            preparedUpdate.setInt(6, pages);
            preparedUpdate.setInt(7, seriesId);
            preparedUpdate.setInt(8, seriesNumber);
            preparedUpdate.setInt(9, year);
            preparedUpdate.setString(10, genre);
            preparedUpdate.setString(11, image);
            preparedUpdate.setString(12, googleId);
            preparedUpdate.setString(13, goodreadsId);
            preparedUpdate.setString(14, isbn);

            final int executeUpdate = preparedUpdate.executeUpdate();

            if (executeUpdate > 0) {
                System.out.println(this.toString() + "\n\n");
                return true;
            }
            return false;
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String toString() {
        return "Book [isbn=" + isbn + ", filename=" + filename + ", author=" + author
                + ", authorId=" + authorId + ", title=" + title + ", rating=" + rating
                + ", synopsis=" + synopsis + ", pages=" + pages + ", seriesId=" + seriesId
                + ", seriesNumber=" + seriesNumber + ", year=" + year + ", genre=" + genre
                + ", image=" + image + ", googleId=" + googleId + ", goodreadsId=" + goodreadsId
                + "]";
    }
}
