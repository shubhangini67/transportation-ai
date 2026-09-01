import React, { useEffect, useState, useCallback } from 'react';
import { FiChevronLeft, FiChevronRight, FiShield, FiUserCheck, FiUserX } from 'react-icons/fi';
import { toast } from 'react-toastify';
import userService from '../../services/userService';
import StatusBadge from '../../components/common/StatusBadge';
import '../../styles/components.css';

const ROLES = ['ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT'];

function UserList() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await userService.getAll({ page, size: pageSize });
      setUsers(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleRoleChange = async (userId, newRole) => {
    try {
      await userService.changeRole(userId, newRole);
      toast.success('Role updated');
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to change role');
    }
  };

  const handleToggleActive = async (user) => {
    try {
      if (user.active) {
        await userService.deactivate(user.id);
        toast.success('User deactivated');
      } else {
        await userService.activate(user.id);
        toast.success('User activated');
      }
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiShield style={{ marginRight: 8 }} /> Access</h2>
      </div>
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Full Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr><td colSpan={7} className="empty-row">No users found</td></tr>
            ) : (
              users.map(user => (
                <tr key={user.id}>
                  <td><strong>{user.username}</strong></td>
                  <td>{user.fullName || '—'}</td>
                  <td>{user.email}</td>
                  <td>
                    <select
                      className="form-input"
                      style={{ padding: '4px 8px', fontSize: '0.8rem', width: 'auto' }}
                      value={user.role}
                      onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    >
                      {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </td>
                  <td><StatusBadge status={user.active ? 'ACTIVE' : 'INACTIVE'} /></td>
                  <td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}</td>
                  <td className="actions-cell">
                    <button
                      className={`btn-icon ${user.active ? 'btn-delete' : 'btn-start'}`}
                      onClick={() => handleToggleActive(user)}
                      title={user.active ? 'Deactivate' : 'Activate'}
                    >
                      {user.active ? <FiUserX /> : <FiUserCheck />}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        {totalPages > 1 && (
          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>
              <FiChevronLeft />
            </button>
            <span>Page {page + 1} of {totalPages} ({totalElements} total)</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
              <FiChevronRight />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default UserList;

