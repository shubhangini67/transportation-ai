import React, { useState } from 'react';
import { FiEdit2, FiTrash2, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import '../../styles/components.css';

const PAGE_SIZE = 10;

function DataTable({ columns, data, onEdit, onDelete, actions, onRowClick }) {
  const [currentPage, setCurrentPage] = useState(1);
  const totalPages = Math.ceil(data.length / PAGE_SIZE);
  const startIdx = (currentPage - 1) * PAGE_SIZE;
  const pageData = data.slice(startIdx, startIdx + PAGE_SIZE);

  return (
    <div className="table-container">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.label}</th>
            ))}
            {(onEdit || onDelete || actions) && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {pageData.length === 0 ? (
            <tr><td colSpan={columns.length + 1} className="empty-row">No data found</td></tr>
          ) : (
            pageData.map((row, idx) => (
              <tr
                key={row.id || idx}
                className={onRowClick ? 'clickable-row' : undefined}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
              >
                {columns.map((col) => (
                  <td key={col.key}>
                    {col.render ? col.render(row[col.key], row) : row[col.key] ?? '—'}
                  </td>
                ))}
                {(onEdit || onDelete || actions) && (
                  <td className="actions-cell" onClick={(e) => e.stopPropagation()}>
                    {actions && actions(row)}
                    {onEdit && (
                      <button className="btn-icon btn-edit" onClick={() => onEdit(row)} title="Edit">
                        <FiEdit2 />
                      </button>
                    )}
                    {onDelete && (
                      <button className="btn-icon btn-delete" onClick={() => onDelete(row)} title="Delete">
                        <FiTrash2 />
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={currentPage === 1} onClick={() => setCurrentPage(p => p - 1)}>
            <FiChevronLeft />
          </button>
          <span>Page {currentPage} of {totalPages}</span>
          <button disabled={currentPage === totalPages} onClick={() => setCurrentPage(p => p + 1)}>
            <FiChevronRight />
          </button>
        </div>
      )}
    </div>
  );
}

export default DataTable;

