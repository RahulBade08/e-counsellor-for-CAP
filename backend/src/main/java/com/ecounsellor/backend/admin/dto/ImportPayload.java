package com.ecounsellor.backend.admin.dto;

import java.util.List;

/**
 * Body of POST /api/admin/import/push.
 *
 * {
 *   "year": "2024",
 *   "rows": [ { "college_code": "06155", ... }, ... ]
 * }
 */
public class ImportPayload {

    private String year;
    private List<ImportRow> rows;

    public String getYear()              { return year; }
    public void setYear(String year)    { this.year = year; }

    public List<ImportRow> getRows()          { return rows; }
    public void setRows(List<ImportRow> rows) { this.rows = rows; }
}
