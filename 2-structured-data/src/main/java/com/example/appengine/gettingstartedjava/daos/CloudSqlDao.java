/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.gettingstartedjava.daos;

import org.apache.commons.dbcp2.BasicDataSource;

import com.example.appengine.gettingstartedjava.objects.Book;
import com.example.appengine.gettingstartedjava.objects.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CloudSqlDao implements BookDao {

  private static final BasicDataSource dataSource = new BasicDataSource();

  public CloudSqlDao() throws SQLException {
    final String url = System.getenv("SQL_DATABASE_URL");
    dataSource.setUrl(url);
    final String createTableSql = "CREATE TABLE IF NOT EXISTS books ( id INT NOT NULL "
        + "AUTO_INCREMENT, author VARCHAR(255), description VARCHAR(255), publishedDate "
        + "VARCHAR(255), title VARCHAR(255), PRIMARY KEY (id))";
    try (Connection conn = dataSource.getConnection()) {
      conn.createStatement().executeUpdate(createTableSql);
    }
  }

  @Override
  public Long createBook(Book book) throws SQLException {
    final String createBookString = "INSERT INTO books (author, description, publishedDate, title) "
        + "VALUES (?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        final PreparedStatement createBookStmt = conn.prepareStatement(createBookString,
            Statement.RETURN_GENERATED_KEYS)) {
      createBookStmt.setString(1, book.getAuthor());
      createBookStmt.setString(2, book.getDescription());
      createBookStmt.setString(3, book.getPublishedDate());
      createBookStmt.setString(4, book.getTitle());
      createBookStmt.executeUpdate();
      try (ResultSet keys = createBookStmt.getGeneratedKeys()) {
        keys.next();
        return keys.getLong(1);
      }
    }
  }

  @Override
  public Book readBook(Long bookId) throws SQLException {
    final String readBookString = "SELECT * FROM books WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement readBookStmt = conn.prepareStatement(readBookString)) {
      readBookStmt.setLong(1, bookId);
      try (ResultSet keys = readBookStmt.executeQuery()) {
        keys.next();
        return new Book.Builder()
            .author(keys.getString("author"))
            .description(keys.getString("description"))
            .id(keys.getLong("id"))
            .publishedDate(keys.getString("publishedDate"))
            .title(keys.getString("title"))
            .build();
      }
    }
  }

  @Override
  public void updateBook(Book book) throws SQLException {
    final String updateBookString = "UPDATE books SET author = ?, description = ?, "
        + "publishedDate = ?, title = ? WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement updateBookStmt = conn.prepareStatement(updateBookString)) {
      updateBookStmt.setString(1, book.getAuthor());
      updateBookStmt.setString(2, book.getDescription());
      updateBookStmt.setString(3, book.getPublishedDate());
      updateBookStmt.setString(4, book.getTitle());
      updateBookStmt.setLong(5, book.getId());
      updateBookStmt.executeUpdate();
    }
  }

  @Override
  public void deleteBook(Long bookId) throws SQLException {
    final String deleteBookString = "DELETE FROM books WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookString)) {
      deleteBookStmt.setLong(1, bookId);
      deleteBookStmt.executeUpdate();
    }
  }

  @Override
  public Result<Book> listBooks(String cursor) throws SQLException {
    int offset = 0;
    if (cursor != null && !cursor.equals("")) {
      offset = Integer.parseInt(cursor);
    }
    final String listBooksString = "SELECT SQL_CALC_FOUND_ROWS author, description, id, "
        + "publishedDate, title FROM books ORDER BY title ASC LIMIT 10 OFFSET ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement listBooksStmt = conn.prepareStatement(listBooksString)) {
      listBooksStmt.setInt(1, offset);
      List<Book> resultBooks = new ArrayList<>();
      try (ResultSet rs = listBooksStmt.executeQuery()) {
        while (rs.next()) {
          Book book = new Book.Builder()
              .author(rs.getString("author"))
              .description(rs.getString("description"))
              .id(rs.getLong("id"))
              .publishedDate(rs.getString("publishedDate"))
              .title(rs.getString("title"))
              .build();
          resultBooks.add(book);
        }
      }
      try (ResultSet rs = conn.createStatement().executeQuery("SELECT FOUND_ROWS()")) {
        int totalNumRows = 0;
        if (rs.next()) {
          totalNumRows = rs.getInt(1);
        }
        if (totalNumRows > offset + 10) {
          return new Result<>(resultBooks, Integer.toString(offset + 10));
        } else {
          return new Result<>(resultBooks);
        }
      }
    }
  }
}