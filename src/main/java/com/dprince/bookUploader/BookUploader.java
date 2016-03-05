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

	public static void main(String[] args) {
		final Book book = setUpInstanceOfBook(args[0]);

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
		final String server = "www.princewalker.com";
		final int port = 21;
		final String user = "prin9762";
		final String pass = "W@lker75";

		log.info("uploadFile()");
		final FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
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
		return true;
	}
}
