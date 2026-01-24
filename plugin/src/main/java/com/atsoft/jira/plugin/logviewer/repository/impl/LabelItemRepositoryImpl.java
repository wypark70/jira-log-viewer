package com.atsoft.jira.plugin.logviewer.repository.impl;

import com.atsoft.jira.plugin.logviewer.dto.LabelItemDto;
import com.atsoft.jira.plugin.logviewer.repository.LabelItemRepository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LabelItemRepositoryImpl implements LabelItemRepository {
    @ComponentImport
    private final DataSource dataSource;

    @Override
    public List<LabelItemDto> getAll() {
        List<LabelItemDto> items = new ArrayList<>();
        String sql = "SELECT \"ID\", \"CUSTOM_FIELD_ID\", \"NAME\", \"PROJECT_ID\" FROM \"AO_5D733E_LABEL_ITITEM\"";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all label items", e);
        }
        return items;
    }

    @Override
    public LabelItemDto getById(int id) {
        String sql = "SELECT \"ID\", \"CUSTOM_FIELD_ID\", \"NAME\", \"PROJECT_ID\" FROM \"AO_5D733E_LABEL_ITITEM\" WHERE \"ID\" = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching label item by id: " + id, e);
        }
        return null;
    }

    @Override
    public LabelItemDto create(String customFieldId, String name, String projectId) {
        String sql = "INSERT INTO \"AO_5D733E_LABEL_ITITEM\" (\"CUSTOM_FIELD_ID\", \"NAME\", \"PROJECT_ID\") VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customFieldId);
            ps.setString(2, name);
            ps.setString(3, projectId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    return getById(generatedId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating label item", e);
        }
        return null;
    }

    @Override
    public LabelItemDto update(int id, String customFieldId, String name, String projectId) {
        String sql = "UPDATE \"AO_5D733E_LABEL_ITITEM\" SET \"CUSTOM_FIELD_ID\" = ?, \"NAME\" = ?, \"PROJECT_ID\" = ? WHERE \"ID\" = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customFieldId);
            ps.setString(2, name);
            ps.setString(3, projectId);
            ps.setInt(4, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                return getById(id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating label item id: " + id, e);
        }
        return null;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM \"AO_5D733E_LABEL_ITITEM\" WHERE \"ID\" = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting label item id: " + id, e);
        }
    }

    private LabelItemDto mapRow(ResultSet rs) throws SQLException {
        return new LabelItemDto(
                rs.getInt("ID"),
                rs.getString("CUSTOM_FIELD_ID"),
                rs.getString("NAME"),
                rs.getString("PROJECT_ID"));
    }
}
