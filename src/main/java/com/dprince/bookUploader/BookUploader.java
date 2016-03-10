package com.dprince.bookUploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BookUploader {
    static Logger log = LogManager.getLogger(BookUploader.class.getName());

    private static final String SERVER = "www.princewalker.com";
    private static final int PORT = 21;
    private static final String USER = "prin9762";
    private static final String PASS = "W@lker75";
    private static Book book;

    public static void main(String[] args) throws IOException {
        book = setUpInstanceOfBook(args[0]);

        // Check if book exists on the server first.
        if (checkFileExists("books/" + book.filename)) {
            log.info("Book exists.");
            SleepForThree();
            System.exit(0);
        }

        // Get list of all isbns related to book title and author from google.
        final List<String> potentialIsbnList = Google.getPotentialIsbnsFromGoogle(book.author,
                book.title);

        // Find an isbn that works with goodreads.
        for (final String isbn : potentialIsbnList) {
            try {
                if (Goodreads.getGoodreadsBookInfo(isbn) == true) {

                    // Add google info.
                    Google.addGoogleInfo();

                    // Insert to database
                    DataBase.insertFromUpload(book);

                    // Check if insert was successful.
                    if (!DataBase.checkIfBookExistsInDB(book.isbn)) {
                        // Uplaod file to server.
                        uploadFile(book.filename, book.fullFilename, book.isbn);
                    } else {
                        log.fatal(
                                "Book was not found in DB after insert, therefore no file upload");
                    }

                    break;
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        final Scanner in = new Scanner(System.in);
        System.out.println("Would you like to add a isbn?");
        final String userResponse = in.nextLine();
        in.close();

        if (userResponse.equalsIgnoreCase("n")) {
            SleepForThree();
            System.exit(0);
        } else {
            try {
                if (Goodreads.getGoodreadsBookInfo(userResponse) == true) {

                    // Add google info.
                    Google.addGoogleInfo();

                    // Insert to database
                    DataBase.insertFromUpload(book);

                    // Check if insert was successful.
                    if (!DataBase.checkIfBookExistsInDB(book.isbn)) {
                        // Upload file to server.
                        uploadFile(book.filename, book.fullFilename, book.isbn);
                    } else {
                        log.fatal(
                                "Book was not found in DB after insert, therefore no file upload");
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        SleepForThree();
        SleepForThree();
        SleepForThree();
    }

    public static void SleepForThree() {
        try {
            System.out.println("Sleeping");
            Thread.sleep(3000);
        } catch (final Exception e) {
        }
    }

    /**
     * @param arg
     *            User input of filename to be uploaded and database populated
     *            with.
     * @return Book with author, title, filename and fullFilename. {@link Book}
     */
    public static Book setUpInstanceOfBook(String arg) {
        final Book book = Book.getInstance();

        final String filename = arg.substring(arg.lastIndexOf("\\") + 1, arg.length());
        log.info(arg);
        final String[] bits = filename.split(" - ");
        final String author = bits[0];
        final String title = bits[1].substring(0, bits[1].lastIndexOf("."));

        book.setAuthor(author);
        book.setTitle(title);
        book.setFilename(filename);
        book.setFullFilename(arg);

        log.info("Setting up book...");
        log.info("Filename: " + filename);
        log.info("Author: " + author + "\nTitle: " + title);
        return book;
    }

    /**
     * @param filename
     * @param fullFileName
     *            filename with path.
     * @return true if upload succeeds, false otherwise.
     */
    public static boolean uploadFile(String filename, String fullFileName, String isbn) {
        log.info("uploadFile()");
        final FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(SERVER, PORT);
            ftpClient.login(USER, PASS);
            ftpClient.enterLocalPassiveMode();

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            final File firstLocalFile = new File(fullFileName);

            final String firstRemoteFile = "books/" + filename;

            final InputStream inputStream = new FileInputStream(firstLocalFile);

            log.info("Start uploading first file, isbn: " + isbn);
            final boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                log.info("The first file is uploaded successfully.");
            }
        } catch (final IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }

        boolean success = false;
        try {
            success = checkFileExists("books/" + filename);
            log.info("Book uploaded successfully");
        } catch (final IOException e) {
            log.info("Book NOT uploaded successfully");
            e.printStackTrace();
        }

        return success;
    }

    static boolean checkFileExists(String filepath) throws IOException {
        final FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(SERVER, PORT);
            ftpClient.login(USER, PASS);
            ftpClient.enterLocalPassiveMode();

            final InputStream inputStream = ftpClient.retrieveFileStream(filepath);
            final Integer returnCode = ftpClient.getReplyCode();
            if (inputStream == null || returnCode == 550) {
                return false;
            }
        } catch (final Exception e) {

        }
        return true;
    }
}
