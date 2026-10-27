package com.networkfinder.database;

import com.networkfinder.model.Connection;
import com.networkfinder.model.ConnectionStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConnectionDao {
    private final DatabaseManager db;

    public ConnectionDao() {
        this.db = DatabaseManager.getInstance();
    }

    public boolean addConnection(String requesterId, String receiverId, ConnectionStatus status) {
        String sql = """
            INSERT INTO connections (requester_id, receiver_id, status, created_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(requester_id, receiver_id) DO UPDATE SET status = excluded.status;
        """;
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requesterId);
            stmt.setString(2, receiverId);
            stmt.setString(3, status.name());
            stmt.setString(4, LocalDateTime.now().toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getAcceptedPairs() {
        List<String[]> pairs = new ArrayList<>();
        String sql = "SELECT requester_id, receiver_id FROM connections WHERE status = 'ACCEPTED';";
        try (java.sql.Connection conn = db.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                pairs.add(new String[]{rs.getString("requester_id"), rs.getString("receiver_id")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pairs;
    }

    public List<Connection> getConnectionsForStudent(String studentId) {
        List<Connection> list = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE requester_id = ? OR receiver_id = ?;";
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean hasConnection(String u, String v) {
        String sql = """
            SELECT 1 FROM connections
            WHERE (requester_id = ? AND receiver_id = ?)
               OR (requester_id = ? AND receiver_id = ?)
            LIMIT 1;
        """;
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u);
            stmt.setString(2, v);
            stmt.setString(3, v);
            stmt.setString(4, u);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeConnection(String u, String v) {
        String sql = """
            DELETE FROM connections
            WHERE (requester_id = ? AND receiver_id = ?)
               OR (requester_id = ? AND receiver_id = ?);
        """;
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u);
            stmt.setString(2, v);
            stmt.setString(3, v);
            stmt.setString(4, u);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Removes every connection row involving the user (used when their account goes away). */
    public boolean deleteConnectionsForUser(String studentId) {
        String sql = "DELETE FROM connections WHERE requester_id = ? OR receiver_id = ?;";
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, studentId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Connection getConnectionBetween(String u, String v) {
        String sql = """
            SELECT * FROM connections
            WHERE (requester_id = ? AND receiver_id = ?)
               OR (requester_id = ? AND receiver_id = ?)
            LIMIT 1;
        """;
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u);
            stmt.setString(2, v);
            stmt.setString(3, v);
            stmt.setString(4, u);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Connection> getPendingIncomingRequests(String studentId) {
        List<Connection> list = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE receiver_id = ? AND status = 'PENDING' ORDER BY created_at DESC;";
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Connection> getPendingOutgoingRequests(String studentId) {
        List<Connection> list = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE requester_id = ? AND status = 'PENDING' ORDER BY created_at DESC;";
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateConnectionStatus(String requesterId, String receiverId, ConnectionStatus status) {
        String sql = "UPDATE connections SET status = ? WHERE requester_id = ? AND receiver_id = ?;";
        try (java.sql.Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, requesterId);
            stmt.setString(3, receiverId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Connection mapRow(ResultSet rs) throws SQLException {
        Connection c = new Connection();
        c.setId(rs.getInt("id"));
        c.setRequesterId(rs.getString("requester_id"));
        c.setReceiverId(rs.getString("receiver_id"));
        c.setStatus(ConnectionStatus.valueOf(rs.getString("status")));
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            try {
                c.setCreatedAt(LocalDateTime.parse(createdAtStr));
            } catch (Exception ignored) {
                c.setCreatedAt(LocalDateTime.now());
            }
        }
        return c;
    }
}
