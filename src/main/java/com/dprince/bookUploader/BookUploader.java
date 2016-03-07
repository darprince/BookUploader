package com.dprince.bookUploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

	public static void main(String[] args) {
		final Book book = setUpInstanceOfBook(args[0]);

		boolean success = false;
		try {
			success = checkFileExists("books/" + book.filename);
			if (success) {
				log.info("Book exists.");
				System.exit(0);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		log.info("getIsbnFromGoogle()");
		final List<String> potentialIsbnList = Google.getPotentialIsbnsFromGoogle(book.author, book.title);

		if (DataBase.insertFromUpload(potentialIsbnList, book)) {
			uploadFile(book.filename, book.fullFilename, book.isbn);
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
		} catch (final IOException e) {
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
